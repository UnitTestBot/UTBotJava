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

    private val collectedValues = mutableMapOf<String, Storage>()
    private val visitor = MatchVisitor(paramNames)

    init {
        visitor.visitFunctionDef(method.ast(), collectedValues)
    }

    fun suggestBasedOnConstants(): List<Set<ClassId>> {
        return method.arguments.map { param ->
            if (param.type == pythonAnyClassId)
                collectedValues[param.name]?.typeStorages?.map { ClassId(it.typeName) }?.toSet() ?: emptySet()
            else
                setOf(param.type)
        }
    }

    fun getConstants(): List<FuzzedConcreteValue> =
        collectedValues.values
            .map { it.typeStorages }
            .flatMap { storageList ->
                storageList.mapNotNull { storage -> storage.const }
            }

    data class TypeStorage(val typeName: String, val const: FuzzedConcreteValue?)
    data class AttributeStorage(val attributeName: String)
    data class Storage(
        val typeStorages: MutableSet<TypeStorage> = mutableSetOf(),
        val attributeStorages: MutableSet<AttributeStorage> = mutableSetOf(),
    )

    private class MatchVisitor(val paramNames: List<String>): ModifierVisitor<MutableMap<String, Storage>>() {

        val knownTypes = PythonTypesStorage.builtinTypes.map { it.name }
        fun <A, N> namePat(): Parser<(String) -> A, A, N> {
            val names: List<Parser<(String) -> A, A, N>> = paramNames.map { paramName ->
                map0(refl(name(equal(paramName))), paramName)
            }
            return names.reduce { acc, elem -> or(acc, elem) }
        }
        fun <A, N> valuePat(op: FuzzedOp = FuzzedOp.NONE): Parser<(TypeStorage) -> A, A, N> {
            val consts = listOf<Parser<(TypeStorage) -> A, A, N>>(
                map1(refl(num(apply()))) { x ->
                    TypeStorage("int", FuzzedConcreteValue(PythonIntModel.classId, BigInteger(x), op)) },
                map1(refl(str(apply()))) { x ->
                    TypeStorage(
                        "str",
                        FuzzedConcreteValue(
                            PythonStrModel.classId,
                            x.removeSurrounding("\"", "\"").removeSurrounding("'", "'"),
                            op
                        )
                    ) },
                map0(refl(true_()),
                    TypeStorage("bool", FuzzedConcreteValue(PythonBoolModel.classId, true, op))),
                map0(refl(false_()),
                    TypeStorage("bool", FuzzedConcreteValue(PythonBoolModel.classId, false, op))),
                // map0(refl(none()), Storage("None")),
                map0(refl(dict(drop(), drop())), TypeStorage("dict", null)),
                map0(refl(list(drop())), TypeStorage("list", null)),
                map0(refl(set(drop())), TypeStorage("set", null)),
                map0(refl(tuple(drop())), TypeStorage("tuple", null))
            )
            val constructors: List<Parser<(TypeStorage) -> A, A, N>> = knownTypes.map { typeName ->
                map0(
                    refl(
                        functionCall(
                            name(
                                equal(typeName)
                            ),
                            drop()
                        )
                    ),
                    TypeStorage(typeName, null)
                )
            }
            return (consts + constructors).reduce { acc, elem -> or(acc, elem) }
        }

        private fun <N> parseAndPut(
            collection: MutableMap<String, Storage>,
            pat: Parser<(String) -> (TypeStorage) -> Pair<String, TypeStorage>?, Pair<String, TypeStorage>?, N>,
            ast: N
        ) {
            parse(
                pat,
                onError = null,
                ast
            ) { paramName -> { storage -> Pair(paramName, storage) } } ?.let {
                val listOfStorage = collection[it.first] ?: Storage()
                listOfStorage.typeStorages.add(it.second)
                collection[it.first] = listOfStorage
            }
        }

        override fun visitAssign(ast: Assign, param: MutableMap<String, Storage>): AST {
            parseAndPut(
                param,
                assign(ftargets = any(namePat()), fvalue = valuePat()),
                ast
            )
            return super.visitAssign(ast, param)
        }

        override fun visitAugAssign(ast: AugAssign, param: MutableMap<String, Storage>): AST {
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

        override fun visitBinOp(ast: BinOp, param: MutableMap<String, Storage>): AST {
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

        fun saveToAttributeStorage(name: AST, methodName: String, param: MutableMap<String, Storage>) {
            paramNames.forEach {
                when (name) {
                    is Name -> {
                        if (name.id.toString() == methodName) {
                            (param[it] ?: Storage()).attributeStorages.add(
                                AttributeStorage(name.id.toString())
                            )
                        }
                    }
                }
            }
        }

        override fun visitGt(gt: Gt, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(gt.left, "__gt__", param)
            saveToAttributeStorage(gt.right, "__gt__", param)
//                param[it]?.attributeStorages?.addAll(getTypesFromStub(gt.left, "__gt__", it))
//                param[it]?.attributeStorages?.addAll(getTypesFromStub(gt.right, "__gt__", it))
            return super.visitGt(gt, param)
        }

        override fun visitGtE(gte: GtE, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(gte.left, "__ge__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(gte.right, "__ge__", it))
            }
            return super.visitGtE(gte, param)
        }

        override fun visitLt(lt: Lt, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(lt.left, "__lt__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(lt.right, "__lt__", it))
            }
            return super.visitLt(lt, param)
        }

        override fun visitLtE(lte: LtE, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(lte.left, "__le__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(lte.right, "__le__", it))
            }
            return super.visitLtE(lte, param)
        }

        override fun visitEq(eq: Eq, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(eq.left, "__eq__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(eq.right, "__eq__", it))
            }
            return super.visitEq(eq, param)
        }

        override fun visitNotEq(ne: NotEq, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(ne.left, "__ne__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(ne.right, "__ne__", it))
            }
            return super.visitNotEq(ne, param)
        }

        override fun visitIn(`in`: In, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(`in`.left, "__contains__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(`in`.right, "__contains__", it))
            }
            return super.visitIn(`in`, param)
        }

        override fun visitIndex(index: Index, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(index.value, "__index__", it))
            }
            return super.visitIndex(index, param)
        }

        override fun visitFloorDiv(floorDiv: FloorDiv, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(floorDiv.left, "__floordiv__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(floorDiv.right, "__floordiv__", it))
            }
            return super.visitFloorDiv(floorDiv, param)
        }

        override fun visitInvert(invert: Invert, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(invert.expression, "__invert__", it))
            }
            return super.visitInvert(invert, param)
        }

        override fun visitLShift(lShift: LShift, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(lShift.left, "__lshift__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(lShift.right, "__lshift__", it))
            }
            return super.visitLShift(lShift, param)
        }

        override fun visitModulo(modulo: Mod, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(modulo.left, "__mod__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(modulo.right, "__mod__", it))
            }
            return super.visitModulo(modulo, param)
        }

        override fun visitMult(mult: Mult, param: MutableMap<String, Storage>): AST {
            paramNames.forEach {
                param[it]?.attributeStorages?.addAll(getTypesFromStub(mult.left, "__mul__", it))
                param[it]?.attributeStorages?.addAll(getTypesFromStub(mult.right, "__mul__", it))
            }
            return super.visitMult(mult, param)
        }

        fun getTypesFromStub(name: AST, methodName: String, paramName: String): MutableSet<AttributeStorage> {
            return when (name) {
                is Name -> {
                    if (name.id.toString() == paramName) {
                        StubFileFinder.findTypeWithMethod(methodName).map {
                            AttributeStorage(it)
                        }.toMutableSet()
                    } else mutableSetOf()
                }
                else -> mutableSetOf()
            }
        }
    }
}