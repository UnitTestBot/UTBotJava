package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
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
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.boolops.Or
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.*
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.Invert
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.UAdd
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.USub
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.Delete
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedOp
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTypesStorage
import java.math.BigInteger

class ArgInfoCollector(val method: PythonMethod) {
    data class TypeStorage(val typeName: String, val const: FuzzedConcreteValue?)
    data class MethodStorage(val methodName: String)
    data class FunctionArgStorage(val name: String, val index: Int)
    data class FieldStorage(val name: String)
    data class Storage(
        val typeStorages: MutableSet<TypeStorage> = mutableSetOf(),
        val methodStorages: MutableSet<MethodStorage> = mutableSetOf(),
        val functionArgStorages: MutableSet<FunctionArgStorage> = mutableSetOf(),
        val fieldStorages: MutableSet<FieldStorage> = mutableSetOf()
    )

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

    fun getFunctionArgs(): List<List<FunctionArgStorage>> =
        collectedValues.values.map { it.functionArgStorages.toList() }

    fun getMethods(): List<List<MethodStorage>> =
        collectedValues.values.map { it.methodStorages.toList() }

    fun getFields(): List<List<FieldStorage>> =
        collectedValues.values.map { it.fieldStorages.toList() }

    private class MatchVisitor(private val paramNames: List<String>): ModifierVisitor<MutableMap<String, Storage>>() {

        private val knownTypes = PythonTypesStorage.builtinTypes.map { it.name }
        private fun <A, N> namePat(): Parser<(String) -> A, A, N> {
            val names: List<Parser<(String) -> A, A, N>> = paramNames.map { paramName ->
                map0(refl(name(equal(paramName))), paramName)
            }
            return names.reduce { acc, elem -> or(acc, elem) }
        }

        private fun getStr(str: String): String {
            val res = str.removeSurrounding("\"")
            return if (res.length == str.length)
                str.removeSurrounding("\'")
            else
                res
        }

        private fun <A, N> valuePat(op: FuzzedOp = FuzzedOp.NONE): Parser<(TypeStorage) -> A, A, N> {
            val consts = listOf<Parser<(TypeStorage) -> A, A, N>>(
                map1(refl(num(apply()))) { x ->
                    TypeStorage("int", FuzzedConcreteValue(PythonIntModel.classId, BigInteger(x), op)) },
                map1(refl(str(apply()))) { x ->
                    TypeStorage("str", FuzzedConcreteValue(PythonStrModel.classId, getStr(x), op)) },
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
                map0(refl(functionCall(name(equal(typeName)), drop())), TypeStorage(typeName, null))
            }
            return (consts + constructors).reduce { acc, elem -> or(acc, elem) }
        }

        private fun addToStorage(
            paramName: String,
            collection: MutableMap<String, Storage>,
            add: (Storage) -> Unit
        ) {
            val storage = collection[paramName] ?: Storage()
            add(storage)
            collection[paramName] = storage
        }

        private fun <N> parseAndPut(
            collection: MutableMap<String, Storage>,
            pat: Parser<(String) -> (TypeStorage) -> Pair<String, TypeStorage>?, Pair<String, TypeStorage>?, N>,
            ast: N
        ) {
            parse(pat, onError = null, ast) { paramName -> { storage -> Pair(paramName, storage) } } ?.let {
                addToStorage(it.first, collection) { storage -> storage.typeStorages.add(it.second) }
            }
        }

        private fun collectFunctionArg(atom: Atom, param: MutableMap<String, Storage>) {
            val argNamePatterns: List<Parser<(String) -> (Int) -> ResFuncArg, ResFuncArg, List<Expression>>> =
                paramNames.map { paramName ->
                    map0(anyIndexed(refl(name(equal(paramName)))), paramName)
                }
            val pat = functionCall(
                fname = name(apply()),
                farguments = arguments(
                    fargs = argNamePatterns.reduce { acc, elem -> or(acc, elem) },
                    drop(), drop(), drop()
                )
            )
            parse(pat, onError = null, atom) { funcName -> { paramName -> { index ->
                Pair(paramName, FunctionArgStorage(funcName, index))
            } } } ?. let {
                addToStorage(it.first, param) { storage -> storage.functionArgStorages.add(it.second) }
            }
        }

        private fun collectField(atom: Atom, param: MutableMap<String, Storage>) {
            val namePatterns: List<Parser<(String) -> (String) -> ResField, (String) -> ResField, Name>> =
                paramNames.map { paramName ->
                    map0(name(equal(paramName)), paramName)
                }
            val pat = classField(
                fname = namePatterns.reduce { acc, elem -> or(acc, elem) },
                fattributeId = apply()
            )
            parse(pat, onError = null, atom) { paramName -> { attributeId ->
                Pair(paramName, FieldStorage(attributeId))
            } } ?.let {
                addToStorage(it.first, param) { storage -> storage.fieldStorages.add(it.second) }
            }
        }

        override fun visitAtom(atom: Atom, param: MutableMap<String, Storage>): AST {
            collectFunctionArg(atom, param)
            collectField(atom, param)
            return super.visitAtom(atom, param)
        }

        override fun visitAssign(ast: Assign, param: MutableMap<String, Storage>): AST {
            parseAndPut(param, assign(ftargets = any(namePat()), fvalue = valuePat()), ast)
            return super.visitAssign(ast, param)
        }

        override fun visitAugAssign(ast: AugAssign, param: MutableMap<String, Storage>): AST {
            parseAndPut(param, augAssign(ftarget = namePat(), fvalue = valuePat(), fop = drop()), ast)
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
                if (name is Name && name.id.name == it) {
                    addToStorage(it, param) { storage ->
                        storage.methodStorages.add(MethodStorage(methodName))
                    }
                }
            }
        }

        override fun visitGt(gt: Gt, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(gt.left, "__gt__", param)
            saveToAttributeStorage(gt.right, "__gt__", param)
            return super.visitGt(gt, param)
        }

        override fun visitGtE(gte: GtE, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(gte.left, "__ge__", param)
            saveToAttributeStorage(gte.right, "__ge__", param)
            return super.visitGtE(gte, param)
        }

        override fun visitLt(lt: Lt, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(lt.left, "__lt__", param)
            saveToAttributeStorage(lt.right, "__lt__", param)
            return super.visitLt(lt, param)
        }

        override fun visitLtE(lte: LtE, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(lte.left, "__le__", param)
            saveToAttributeStorage(lte.right, "__le__", param)
            return super.visitLtE(lte, param)
        }

        override fun visitEq(eq: Eq, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(eq.left, "__eq__", param)
            saveToAttributeStorage(eq.right, "__eq__", param)
            return super.visitEq(eq, param)
        }

        override fun visitNotEq(ne: NotEq, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(ne.left, "__ne__", param)
            saveToAttributeStorage(ne.right, "__ne__", param)
            return super.visitNotEq(ne, param)
        }

        override fun visitIn(`in`: In, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(`in`.left, "__contains__", param)
            saveToAttributeStorage(`in`.right, "__contains__", param)
            return super.visitIn(`in`, param)
        }

        override fun visitIndex(index: Index, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(index.value, "__index__", param)
            return super.visitIndex(index, param)
        }

        override fun visitFloorDiv(floorDiv: FloorDiv, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(floorDiv.left, "__floordiv__", param)
            saveToAttributeStorage(floorDiv.right, "__floordiv__", param)
            return super.visitFloorDiv(floorDiv, param)
        }

        override fun visitInvert(invert: Invert, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(invert.expression, "__invert__", param)
            return super.visitInvert(invert, param)
        }

        override fun visitLShift(lShift: LShift, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(lShift.left, "__lshift__", param)
            saveToAttributeStorage(lShift.right, "__lshift__", param)
            return super.visitLShift(lShift, param)
        }

        override fun visitModulo(modulo: Mod, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(modulo.left, "__mod__", param)
            saveToAttributeStorage(modulo.right, "__mod__", param)
            return super.visitModulo(modulo, param)
        }

        override fun visitMult(mult: Mult, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(mult.left, "__mul__", param)
            saveToAttributeStorage(mult.right, "__mul__", param)
            return super.visitMult(mult, param)
        }

        override fun visitUSub(uSub: USub, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(uSub.expression, "__neg__", param)
            return super.visitUSub(uSub, param)
        }

        override fun visitOr(or: Or, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(or.left, "__or__", param)
            saveToAttributeStorage(or.right, "__or__", param)
            return super.visitOr(or, param)
        }

        override fun visitUAdd(uAdd: UAdd, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(uAdd.expression, "__pos__", param)
            return super.visitUAdd(uAdd, param)
        }

        override fun visitPow(pow: Pow, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(pow.left, "__pow__", param)
            saveToAttributeStorage(pow.right, "__pow__", param)
            return super.visitPow(pow, param)
        }

        override fun visitRShift(rShift: RShift, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(rShift.left, "__rshift__", param)
            saveToAttributeStorage(rShift.right, "__rshift__", param)
            return super.visitRShift(rShift, param)
        }

        override fun visitSub(sub: Sub, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(sub.left, "__sub__", param)
            saveToAttributeStorage(sub.right, "__sub__", param)
            return super.visitSub(sub, param)
        }

        override fun visitAdd(add: Add, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(add.left, "__add__", param)
            saveToAttributeStorage(add.right, "__add__", param)
            return super.visitAdd(add, param)
        }

        override fun visitDiv(div: Div, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(div.left, "__truediv__", param)
            saveToAttributeStorage(div.right, "__truediv__", param)
            return super.visitDiv(div, param)
        }

        override fun visitBitXor(bitXor: BitXor, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(bitXor.left, "__xor__", param)
            saveToAttributeStorage(bitXor.right, "__xor__", param)
            return super.visitBitXor(bitXor, param)
        }

        override fun visitDelete(delete: Delete, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(delete.expression, "__delitem__", param)
            return super.visitDelete(delete, param)
        }
    }
}

typealias ResFuncArg = Pair<String, ArgInfoCollector.FunctionArgStorage>?
typealias ResField = Pair<String, ArgInfoCollector.FieldStorage>?