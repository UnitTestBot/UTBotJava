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
import io.github.danielnaczo.python3parser.model.expr.atoms.Num
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.operators.Operator
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.*
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.boolops.Or
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.*
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.*
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.Delete
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.apache.commons.lang3.math.NumberUtils
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedOp
import org.utbot.python.PythonMethod
import org.utbot.python.typing.PythonTypesStorage
import java.math.BigDecimal
import java.math.BigInteger

class ArgInfoCollector(val method: PythonMethod, val argumentTypes: List<PythonClassId>) {
    interface BaseStorage { val name: String }
    data class TypeStorage(override val name: String): BaseStorage
    data class MethodStorage(override val name: String): BaseStorage
    data class FunctionArgStorage(override val name: String, val index: Int): BaseStorage
    data class FunctionRetStorage(override val name: String): BaseStorage
    data class FieldStorage(override val name: String): BaseStorage
    data class Storage(
        val typeStorages: MutableSet<TypeStorage> = mutableSetOf(),
        val methodStorages: MutableSet<MethodStorage> = mutableSetOf(),
        val functionArgStorages: MutableSet<FunctionArgStorage> = mutableSetOf(),
        val fieldStorages: MutableSet<FieldStorage> = mutableSetOf(),
        val functionRetStorages: MutableSet<FunctionRetStorage> = mutableSetOf()
    ) {
        fun toList(): List<BaseStorage> {
            return listOf(
                typeStorages,
                methodStorages,
                functionArgStorages,
                fieldStorages,
                functionRetStorages
            ).flatten()
        }
    }

    private val paramNames = method.arguments.mapIndexedNotNull { index, param ->
        if (argumentTypes[index] == pythonAnyClassId) param.name else null
    }
    private val collectedValues = mutableMapOf<String, Storage>()
    private val visitor = MatchVisitor(paramNames, mutableSetOf())

    init {
        visitor.visitFunctionDef(method.ast(), collectedValues)
    }

    fun getConstants(): List<FuzzedConcreteValue> = visitor.constStorage.toList()

    fun getAllStorages(): Map<String, List<BaseStorage>> {
       return collectedValues.entries.associate { (argName, storage) ->
           argName to storage.toList()
       }
    }

    fun getConstantAnnotations(): Map<String, Set<TypeStorage>> =
        collectedValues.entries.associate {
            it.key to it.value.typeStorages
        }

    fun getFunctionArgs(): Map<String, Set<FunctionArgStorage>> =
        collectedValues.entries.associate {
            it.key to it.value.functionArgStorages }

    fun getMethods(): Map<String, Set<MethodStorage>> =
        collectedValues.entries.associate {
            it.key to it.value.methodStorages
        }

    fun getFields(): Map<String, Set<FieldStorage>> =
        collectedValues.entries.associate {
            it.key to it.value.fieldStorages
        }
    fun getFunctionRets() =
        collectedValues.entries.associate {
            it.key to it.value.functionRetStorages
        }

    private class MatchVisitor(
        private val paramNames: List<String>,
        val constStorage: MutableSet<FuzzedConcreteValue>
    ): ModifierVisitor<MutableMap<String, Storage>>() {

        private fun <A, N> namePat(): Parser<(String) -> A, A, N> {
            val names: List<Parser<(String) -> A, A, N>> = paramNames.map { paramName ->
                map0(refl(name(equal(paramName))), paramName)
            }
            return names.fold(reject()) { acc, elem -> or(acc, elem) }
        }

        private fun getStr(str: String): String {
            val res = str.removeSurrounding("\"")
            return if (res.length == str.length)
                str.removeSurrounding("\'")
            else
                res
        }

        private fun <A> typedExpr(atom: Parser<A, A, Expression>): Parser<A, A, Expression> =
            opExpr(refl(atom), refl(name(drop())))

        private fun <A> typedExpressionPat(): Parser<(TypeStorage) -> A, A, Expression> {
            // map must preserve order
            val typeMap = linkedMapOf<String, Parser<A, A, Expression>>(
                "builtins.int" to refl(num(int())),
                "builtins.float" to refl(num(drop())),
                "builtins.str" to refl(str(drop())),
                "builtins.bool" to or(refl(true_()), refl(false_())),
                "types.NoneType" to refl(none()),
                "builtins.dict" to refl(dict(drop(), drop())),
                "builtins.list" to refl(list(drop())),
                "builtins.set" to refl(set(drop())),
                "builtins.tuple" to refl(tuple(drop()))
            )
            PythonTypesStorage.builtinTypes.forEach { typeNameWithoutPrefix ->
                val typeNameWithPrefix = "builtins.$typeNameWithoutPrefix"
                if (typeMap.containsKey(typeNameWithPrefix))
                    typeMap[typeNameWithPrefix] = or(
                        typeMap[typeNameWithPrefix]!!,
                        refl(functionCallWithoutPrefix(name(equal(typeNameWithoutPrefix)), drop()))
                    )
                else
                    typeMap[typeNameWithPrefix] = refl(functionCallWithoutPrefix(name(equal(typeNameWithoutPrefix)), drop()))
            }
            return typeMap.entries.fold(reject()) { acc, entry ->
                or(acc, map0(typedExpr(entry.value), TypeStorage(entry.key)))
            }
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

        private fun <N> parseAndPutType(
            collection: MutableMap<String, Storage>,
            pat: Parser<(String) -> (TypeStorage) -> Pair<String, TypeStorage>?, Pair<String, TypeStorage>?, N>,
            ast: N
        ) {
            parse(pat, onError = null, ast) { paramName -> { storage -> Pair(paramName, storage) } } ?.let {
                addToStorage(it.first, collection) { storage -> storage.typeStorages.add(it.second) }
            }
        }

        private fun <N> parseAndPutFunctionRet(
            collection: MutableMap<String, Storage>,
            pat: Parser<(String) -> (FunctionRetStorage) -> ResFuncRet, ResFuncRet, N>,
            ast: N
        ) {
            parse(pat, onError = null, ast) { paramName -> { storage -> Pair(paramName, storage) } } ?.let {
                addToStorage(it.first, collection) { storage -> storage.functionRetStorages.add(it.second) }
            }
        }

        private fun collectFunctionArg(atom: Atom, param: MutableMap<String, Storage>) {
            val argNamePatterns: List<Parser<(String) -> (Int) -> ResFuncArg, ResFuncArg, List<Expression>>> =
                paramNames.map { paramName ->
                    map0(anyIndexed(refl(name(equal(paramName)))), paramName)
                }
            val argPat: Parser<(String) -> (Int) -> ResFuncArg, ResFuncArg, List<Expression>> =
                argNamePatterns.fold(reject()) { acc, elem -> or(acc, elem) }
            val pat = functionCallWithPrefix(
                fprefix = drop(),
                fid = apply(),
                farguments = arguments(
                    fargs = argPat,
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
            val pat = classField(
                fname = namePat<(String) -> ResField, Name>(),
                fattributeId = apply()
            )
            parse(pat, onError = null, atom) { paramName -> { attributeId ->
                Pair(paramName, FieldStorage(attributeId))
            } } ?.let {
                addToStorage(it.first, param) { storage -> storage.fieldStorages.add(it.second) }
            }
        }

        private fun <A> subscriptPat(): Parser<(String) -> A, A, Atom> =
            atom(
                fatomElement = namePat(),
                ftrailers = first(refl(index(drop())))
            )

        private fun collectAtomMethod(atom: Atom, param: MutableMap<String, Storage>) {
            val methodPat = classMethod(
                fname = namePat<(String) -> ResMethod, Name>(),
                fattributeId = apply(),
                farguments = drop()
            )
            val getPat = swap(map0(subscriptPat<ResMethod>(), "__getitem__"))
            val pat = or(methodPat, getPat)
            parse(pat, onError = null, atom) { paramName -> { attributeId ->
                Pair(paramName, MethodStorage(attributeId))
            } } ?.let {
                addToStorage(it.first, param) { storage -> storage.methodStorages.add(it.second) }
            }
        }

        private val magicMethodOfFunctionCall: Map<String, String> =
            mapOf(
                "len" to "__len__",
                "str" to "__str__",
                "repr" to "__repr__",
                "bytes" to "__bytes__",
                "format" to "__format__",
                "hash" to "__hash__",
                "bool" to "__bool__",
                "dir" to "__dir__"
            )

        private fun collectMagicMethodsFromCalls(atom: Atom, param: MutableMap<String, Storage>) {
            val callNamePat: Parser<(String) -> (String) -> ResMethod, (String) -> ResMethod, Name> =
                magicMethodOfFunctionCall.entries.fold(reject()) { acc, entry ->
                    or(acc, map0(name(equal(entry.key)), entry.value))
                }
            val pat = functionCallWithoutPrefix(
                fname = callNamePat,
                farguments = arguments(
                    fargs = any(namePat()),
                    drop(), drop(), drop()
                )
            )
            parse(pat, onError = null, atom) { methodName -> { paramName ->
                Pair(paramName, MethodStorage(methodName))
            } } ?.let {
                addToStorage(it.first, param) { storage -> storage.methodStorages.add(it.second) }
            }
        }

        override fun visitAtom(atom: Atom, param: MutableMap<String, Storage>): AST {
            collectFunctionArg(atom, param)
            collectField(atom, param)
            collectAtomMethod(atom, param)
            collectMagicMethodsFromCalls(atom, param)
            return super.visitAtom(atom, param)
        }

        private fun collectTypes(assign: Assign, param: MutableMap<String, Storage>) {
            val pat: Parser<(String) -> (TypeStorage) -> ResAssign, List<ResAssign>, Assign> = assignAll(
                ftargets = allMatches(namePat()), fvalue = typedExpressionPat()
            )
            parse(
                pat,
                onError = emptyList(),
                assign
            ) { paramName -> { typeStorage -> Pair(paramName, typeStorage) } } .map {
                addToStorage(it.first, param) { storage -> storage.typeStorages.add(it.second) }
            }
        }

        private fun collectSetItem(assign: Assign, param: MutableMap<String, Storage>) {
            val setItemPat: Parser<(String) -> String, List<String>, Assign> = assignAll(
                ftargets = allMatches(refl(subscriptPat())),
                fvalue = drop()
            )
            val setItemParams = parse(setItemPat, onError = emptyList(), assign) { it }
            setItemParams.map { paramName ->
                addToStorage(paramName, param) { storage ->
                    storage.methodStorages.add(MethodStorage("__setitem__"))
                }
            }
        }

        private fun <A, N> funcCallNamePat(): Parser<(FunctionRetStorage) -> A, A, N> =
            map1(refl(functionCallWithPrefix(
                fprefix = drop(),
                fid = apply(),
                farguments = drop()
            ))) { x -> FunctionRetStorage(x) }

        private fun collectFuncRet(assign: Assign, param: MutableMap<String, Storage>) {
            val pat: Parser<(String) -> (FunctionRetStorage) -> ResFuncRet, List<ResFuncRet>, Assign> = assignAll(
                ftargets = allMatches(namePat()),
                fvalue = funcCallNamePat()
            )
            val functionRets = parse(pat, onError = emptyList(), assign) { paramName ->
                { functionStorage -> Pair(paramName, functionStorage) }
            }
            functionRets.forEach {
                if (it != null)
                    addToStorage(it.first, param) { storage -> storage.functionRetStorages.add(it.second) }
            }
        }

        override fun visitAssign(ast: Assign, param: MutableMap<String, Storage>): AST {
            collectTypes(ast, param)
            collectSetItem(ast, param)
            collectFuncRet(ast, param)
            return super.visitAssign(ast, param)
        }

        private fun getOpMagicMethod(op: Operator?) =
            when (op) {
                is Gt -> "__gt__"
                is GtE -> "__ge__"
                is Lt -> "__lt__"
                is LtE -> "__le__"
                is Eq -> "__eq__"
                is NotEq -> "__ne__"
                is In -> "__contains__"
                is FloorDiv -> "__floordiv__"
                is Invert -> "__invert__"
                is LShift -> "__lshift__"
                is Mod -> "__mod__"
                is Mult -> "__mul__"
                is USub -> "__neg__"
                is Or -> "__or__"
                is UAdd -> "__pos__"
                is Pow -> "__pow__"
                is RShift -> "__rshift__"
                is Sub -> "__sub__"
                is Add -> "__add__"
                is Div -> "__truediv__"
                is BitXor -> "__xor__"
                is Not -> "__not__"
                else -> null
            }

        override fun visitAugAssign(ast: AugAssign, param: MutableMap<String, Storage>): AST {
            parseAndPutType(param, augAssign(ftarget = namePat(), fvalue = typedExpressionPat(), fop = drop()), ast)
            parseAndPutFunctionRet(param, augAssign(ftarget = namePat(), fvalue = funcCallNamePat(), fop = drop()), ast)
            saveToAttributeStorage(ast.target, getOpMagicMethod(ast.op), param)
            saveToAttributeStorage(ast.value, getOpMagicMethod(ast.op), param)
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

        private fun getNumFuzzedValue(num: String, op: FuzzedOp = FuzzedOp.NONE): FuzzedConcreteValue? =
            try {
                when (val x = NumberUtils.createNumber(num)) {
                    is Int -> FuzzedConcreteValue(PythonIntModel.classId, x.toBigInteger(), op)
                    is Long -> FuzzedConcreteValue(PythonIntModel.classId, x.toBigInteger(), op)
                    is BigInteger -> FuzzedConcreteValue(PythonIntModel.classId, x, op)
                    else -> FuzzedConcreteValue(PythonFloatModel.classId, BigDecimal(num), op)
                }
            } catch (e: NumberFormatException) {
                null
            }

        private fun <A, N> constPat(op: FuzzedOp): Parser<(FuzzedConcreteValue?) -> A, A, N> {
            val pats = listOf<Parser<(FuzzedConcreteValue?) -> A, A, N>>(
                map1(refl(num(apply()))) { x -> getNumFuzzedValue(x, op) },
                map1(refl(str(apply()))) { x ->
                    FuzzedConcreteValue(PythonStrModel.classId, getStr(x), op)
                },
                map0(
                    refl(true_()),
                    FuzzedConcreteValue(PythonBoolModel.classId, true, op)
                ),
                map0(
                    refl(false_()),
                    FuzzedConcreteValue(PythonBoolModel.classId, false, op)
                )
            )
            return pats.reduce { acc, elem -> or(acc, elem) }
        }

        override fun visitNum(num: Num, param: MutableMap<String, Storage>): AST {
            val value = getNumFuzzedValue(num.n)
            if (value != null)
                constStorage.add(value)
            return super.visitNum(num, param)
        }

        override fun visitStr(str: Str, param: MutableMap<String, Storage>?): AST {
            constStorage.add(FuzzedConcreteValue(PythonStrModel.classId, getStr(str.s)))
            return super.visitStr(str, param)
        }

        override fun visitBinOp(ast: BinOp, param: MutableMap<String, Storage>): AST {
            parseAndPutType(
                param,
                or(
                    binOp(fleft = namePat(), fright = typedExpressionPat()),
                    swap(binOp(fleft = typedExpressionPat(), fright = namePat()))
                ),
                ast
            )
            parseAndPutFunctionRet(
                param,
                or(
                    binOp(fleft = namePat(), fright = funcCallNamePat()),
                    swap(binOp(fleft = funcCallNamePat(), fright = namePat()))
                ),
                ast
            )
            val op = getOp(ast)
            if (op != FuzzedOp.NONE)
                parse(
                    binOp(fleft = refl(name(drop())), fright = constPat(op)),
                    onError = null,
                    ast
                ) { it } ?.let { constStorage.add(it) }

            saveToAttributeStorage(ast.left, getOpMagicMethod(ast), param)
            saveToAttributeStorage(ast.right, getOpMagicMethod(ast), param)
            return super.visitBinOp(ast, param)
        }

        fun saveToAttributeStorage(name: AST?, methodName: String?, param: MutableMap<String, Storage>) {
            if (methodName == null)
                return
            paramNames.forEach {
                if (name is Name && name.id.name == it) {
                    addToStorage(it, param) { storage ->
                        storage.methodStorages.add(MethodStorage(methodName))
                    }
                }
            }
        }

        override fun visitUnaryOp(unaryOp: UnaryOp, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(unaryOp.expression, getOpMagicMethod(unaryOp), param)
            return super.visitUnaryOp(unaryOp, param)
        }

        override fun visitDelete(delete: Delete, param: MutableMap<String, Storage>): AST {
            saveToAttributeStorage(delete.expression, "__delitem__", param)
            return super.visitDelete(delete, param)
        }
    }
}

typealias ResFuncArg = Pair<String, ArgInfoCollector.FunctionArgStorage>?
typealias ResField = Pair<String, ArgInfoCollector.FieldStorage>?
typealias ResMethod = Pair<String, ArgInfoCollector.MethodStorage>?
typealias ResAssign = Pair<String, ArgInfoCollector.TypeStorage>
typealias ResFuncRet = Pair<String, ArgInfoCollector.FunctionRetStorage>?