package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.BinOp
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.Eq
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.Gt
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.GtE
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.Lt
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.LtE
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.NotEq
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.subscripts.Index
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.*
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.*
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.Invert
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedOp
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTypesStorage
import java.math.BigInteger
import org.utbot.python.StubFileFinder

class ConstantCollector(val method: PythonMethod) {
    private val paramNames = method.arguments.mapNotNull { param ->
        if (param.type == pythonAnyClassId) param.name else null
    }

    private val collectedValues = mutableMapOf<String, MutableList<Storage>>()
    private val visitor = MatchVisitor(paramNames)

    init {
        visitor.visitFunctionDef(method.ast(), collectedValues)
    }

    fun suggestBasedOnConstants(): List<Set<ClassId>> {
        return method.arguments.map { param ->
            if (param.type == pythonAnyClassId)
                collectedValues[param.name]?.map { ClassId(it.typeName) }?.toSet() ?: emptySet()
            else
                setOf(param.type)
        }
    }

    fun getConstants(): List<FuzzedConcreteValue> =
        collectedValues.values.flatMap { storageList ->
            storageList.mapNotNull { storage -> storage.const }
        }

    data class Storage(val typeName: String, val const: FuzzedConcreteValue?)

    private class MatchVisitor(val paramNames: List<String>): ModifierVisitor<MutableMap<String, MutableList<Storage>>>() {

        val knownTypes = PythonTypesStorage.builtinTypes.map { it.name }
        fun <A, N> namePat(): Parser<(String) -> A, A, N> {
            val names: List<Parser<(String) -> A, A, N>> = paramNames.map { paramName ->
                map0(refl(name(equal(paramName))), paramName)
            }
            return names.reduce { acc, elem -> or(acc, elem) }
        }
        fun <A, N> valuePat(op: FuzzedOp = FuzzedOp.NONE): Parser<(Storage) -> A, A, N> {
            val consts = listOf<Parser<(Storage) -> A, A, N>>(
                map1(refl(num(apply()))) { x ->
                    Storage("int", FuzzedConcreteValue(PythonIntModel.classId, BigInteger(x), op)) },
                map1(refl(str(apply()))) { x ->
                    Storage(
                        "str",
                        FuzzedConcreteValue(
                            PythonStrModel.classId,
                            x.removeSurrounding("\"", "\"").removeSurrounding("'", "'"),
                            op
                        )
                    ) },
                map0(refl(true_()),
                    Storage("bool", FuzzedConcreteValue(PythonBoolModel.classId, true, op))),
                map0(refl(false_()),
                    Storage("bool", FuzzedConcreteValue(PythonBoolModel.classId, false, op))),
                // map0(refl(none()), Storage("None")),
                map0(refl(dict(drop(), drop())), Storage("dict", null)),
                map0(refl(list(drop())), Storage("list", null)),
                map0(refl(set(drop())), Storage("set", null)),
                map0(refl(tuple(drop())), Storage("tuple", null))
            )
            val constructors: List<Parser<(Storage) -> A, A, N>> = knownTypes.map { typeName ->
                map0(refl(functionCall(name(equal(typeName)), drop())), Storage(typeName, null))
            }
            return (consts + constructors).reduce { acc, elem -> or(acc, elem) }
        }

        private fun <N> parseAndPut(
            collection: MutableMap<String, MutableList<Storage>>,
            pat: Parser<(String) -> (Storage) -> Pair<String, Storage>?, Pair<String, Storage>?, N>,
            ast: N
        ) {
            parse(
                pat,
                onError = null,
                ast
            ) { paramName -> { storage -> Pair(paramName, storage) } } ?.let {
                val listOfStorage = collection[it.first]
                listOfStorage?.add(it.second) ?: collection.put(it.first, mutableListOf(it.second))
            }
        }

        override fun visitAssign(ast: Assign, param: MutableMap<String, MutableList<Storage>>): AST {
            parseAndPut(
                param,
                assign(ftargets = any(namePat()), fvalue = valuePat()),
                ast
            )
            return super.visitAssign(ast, param)
        }

        override fun visitAugAssign(ast: AugAssign, param: MutableMap<String, MutableList<Storage>>): AST {
            parseAndPut(
                param,
                augAssign(ftarget = namePat(), fvalue = valuePat(), fop = drop()),
                ast
            )
            return super.visitAugAssign(ast, param)
        }

        private fun getOp(ast: BinOp): FuzzedOp =
            when (ast) {
                is Eq -> FuzzedOp.EQ
                is NotEq -> FuzzedOp.NE
                is Gt -> FuzzedOp.GT
                is GtE -> FuzzedOp.GE
                is Lt -> FuzzedOp.LT
                is LtE -> FuzzedOp.LE
                else -> FuzzedOp.NONE
            }

        override fun visitBinOp(ast: BinOp, param: MutableMap<String, MutableList<Storage>>): AST {
            parseAndPut(
                param,
                or(
                    binOp(fleft = namePat(), fright = valuePat(getOp(ast))),
                    swap(binOp(fleft = valuePat(getOp(ast)), fright = namePat()))
                ),
                ast
            )
            return super.visitBinOp(ast, param)
        }

        override fun visitGt(gt: Gt, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(gt.left, "__gt__"))
            param.addAll(getTypesFromStub(gt.right, "__gt__"))
            return super.visitGt(gt, param)
        }

        override fun visitGtE(gte: GtE, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(gte.left, "__ge__"))
            param.addAll(getTypesFromStub(gte.right, "__ge__"))
            return super.visitGtE(gte, param)
        }

        override fun visitLt(lt: Lt, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(lt.left, "__lt__"))
            param.addAll(getTypesFromStub(lt.right, "__lt__"))
            return super.visitLt(lt, param)
        }

        override fun visitLtE(lte: LtE, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(lte.left, "__le__"))
            param.addAll(getTypesFromStub(lte.right, "__le__"))
            return super.visitLtE(lte, param)
        }

        override fun visitEq(eq: Eq, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(eq.left, "__eq__"))
            param.addAll(getTypesFromStub(eq.right, "__eq__"))
            return super.visitEq(eq, param)
        }

        override fun visitNotEq(ne: NotEq, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(ne.left, "__ne__"))
            param.addAll(getTypesFromStub(ne.right, "__ne__"))
            return super.visitNotEq(ne, param)
        }

        override fun visitIn(`in`: In, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(`in`.left, "__contains__"))
            param.addAll(getTypesFromStub(`in`.right, "__contains__"))
            return super.visitIn(`in`, param)
        }

        override fun visitIndex(index: Index, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(index.value, "__index__"))
            return super.visitIndex(index, param)
        }

        override fun visitFloorDiv(floorDiv: FloorDiv, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(floorDiv.left, "__floordiv__"))
            param.addAll(getTypesFromStub(floorDiv.right, "__floordiv__"))
            return super.visitFloorDiv(floorDiv, param)
        }

        override fun visitInvert(invert: Invert, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(invert.expression, "__invert__"))
            return super.visitInvert(invert, param)
        }

        override fun visitLShift(lShift: LShift, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(lShift.left, "__lshift__"))
            param.addAll(getTypesFromStub(lShift.right, "__lshift__"))
            return super.visitLShift(lShift, param)
        }

        override fun visitModulo(modulo: Mod, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(modulo.left, "__mod__"))
            param.addAll(getTypesFromStub(modulo.right, "__mod__"))
            return super.visitModulo(modulo, param)
        }

        override fun visitMult(mult: Mult, param: MutableCollection<Storage>): AST {
            param.addAll(getTypesFromStub(mult.left, "__mul__"))
            param.addAll(getTypesFromStub(mult.right, "__mul__"))
            return super.visitModulo(mult, param)
        }

        fun getTypesFromStub(name: AST, methodName: String): Collection<Storage> {
            return when (name) {
                is Name -> {
                    if (name.id.toString() == paramName) {
                        StubFileFinder.findTypeWithMethod(methodName).map {
                            Storage(it)
                        }
                    } else emptyList()
                }
                else -> emptyList()
            }
        }
    }
}