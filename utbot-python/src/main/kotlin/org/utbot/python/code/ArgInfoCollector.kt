package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Num
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.comprehensions.Comprehension
import io.github.danielnaczo.python3parser.model.expr.datastructures.Dict
import io.github.danielnaczo.python3parser.model.expr.datastructures.ListExpr
import io.github.danielnaczo.python3parser.model.expr.operators.Operator
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.*
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.boolops.Or
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.*
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.*
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.forStmts.For
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.Delete
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.apache.commons.lang3.math.NumberUtils
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedContext
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonBoolModel
import org.utbot.python.framework.api.python.PythonListModel
import org.utbot.python.framework.api.python.PythonDictModel
import org.utbot.python.framework.api.python.PythonSetModel
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.typing.PythonTypesStorage
import java.math.BigDecimal
import java.math.BigInteger

class ArgInfoCollector(val method: PythonMethod, private val argumentTypes: List<NormalizedPythonAnnotation>) {
    sealed class Hint

    class Type(val type: PythonClassId) : Hint()
    data class Method(val name: String) : Hint()
    data class FunctionArg(val name: String, val index: Int) : Hint()
    data class FunctionRet(val name: String) : Hint()
    data class Field(val name: String) : Hint()
    data class Function(val name: String) : Hint()

    data class ArgInfoStorage(
        val types: MutableSet<Type> = mutableSetOf(),
        val methods: MutableSet<Method> = mutableSetOf(),
        val functionArgs: MutableSet<FunctionArg> = mutableSetOf(),
        val fields: MutableSet<Field> = mutableSetOf(),
        val functionRets: MutableSet<FunctionRet> = mutableSetOf()
    ) {
        fun toList(): List<Hint> {
            return listOf(
                types,
                methods,
                functionArgs,
                fields,
                functionRets
            ).flatten()
        }
    }

    data class GeneralStorage(
        val types: MutableList<Type> = mutableListOf(),
        val functions: MutableSet<Function> = mutableSetOf(),
        val fields: MutableSet<Field> = mutableSetOf(),
        val methods: MutableSet<Method> = mutableSetOf()
    ) {
        fun toList(): List<Hint> {
            return listOf(
                types,
                functions,
                fields,
                methods
            ).flatten()
        }
    }

    private val paramNames = method.arguments.mapIndexedNotNull { index, param ->
        if (argumentTypes[index] == pythonAnyClassId) param.name else null
    }

    private val collectedValues = mutableMapOf<String, ArgInfoStorage>()

    private val visitor = MatchVisitor(paramNames, mutableSetOf(), GeneralStorage())

    init {
        visitor.visitFunctionDef(method.oldAst, collectedValues)
    }

    fun getConstants(): List<FuzzedConcreteValue> = visitor.constStorage.toList()

    fun getAllGeneralHints(): List<Hint> = visitor.generalStorage.toList()

    fun getAllArgHints(): Map<String, List<Hint>> {
        return paramNames.associateWith { argName -> (collectedValues[argName]?.toList() ?: emptyList()) }
    }

    private class MatchVisitor(
        private val paramNames: List<String>,
        val constStorage: MutableSet<FuzzedConcreteValue>,
        val generalStorage: GeneralStorage
    ) : ModifierVisitor<MutableMap<String, ArgInfoStorage>>() {

        private fun <A, N> namePat(): Pattern<(String) -> A, A, N> {
            val names: List<Pattern<(String) -> A, A, N>> = paramNames.map { paramName ->
                map0(refl(name(equal(paramName))), paramName)
            }
            return names.fold(reject()) { acc, elem -> or(acc, elem) }
        }

        private fun <A> typedExpr(atom: Pattern<A, A, Expression>): Pattern<A, A, Expression> =
            opExpr(refl(atom), refl(name(drop())))

        private fun <A> typedExpressionPat(): Pattern<(Type) -> A, A, Expression> {
            // map must preserve order
            val typeMap = linkedMapOf<String, Pattern<A, A, Expression>>(
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
                    typeMap[typeNameWithPrefix] =
                        refl(functionCallWithoutPrefix(name(equal(typeNameWithoutPrefix)), drop()))
            }
            return typeMap.entries.fold(reject()) { acc, entry ->
                or(acc, map0(typedExpr(entry.value), Type(PythonClassId(entry.key))))
            }
        }

        private fun addToStorage(
            paramName: String,
            collection: MutableMap<String, ArgInfoStorage>,
            add: (ArgInfoStorage) -> Unit
        ) {
            val argInfoStorage = collection[paramName] ?: ArgInfoStorage()
            add(argInfoStorage)
            collection[paramName] = argInfoStorage
        }

        private fun <N> parseAndPutType(
            collection: MutableMap<String, ArgInfoStorage>,
            pat: Pattern<(String) -> (Type) -> Pair<String, Type>?, Pair<String, Type>?, N>,
            ast: N
        ) {
            parse(pat, onError = null, ast) { paramName -> { storage -> Pair(paramName, storage) } }?.let {
                addToStorage(it.first, collection) { storage -> storage.types.add(it.second) }
            }
        }

        private fun <N> parseAndPutFunctionRet(
            collection: MutableMap<String, ArgInfoStorage>,
            pat: Pattern<(String) -> (FunctionRet) -> ResFuncRet, ResFuncRet, N>,
            ast: N
        ) {
            parse(pat, onError = null, ast) { paramName -> { storage -> Pair(paramName, storage) } }?.let {
                addToStorage(it.first, collection) { storage -> storage.functionRets.add(it.second) }
            }
        }

        private fun collectFunctionArg(atom: Atom, param: MutableMap<String, ArgInfoStorage>) {
            val argNamePatterns: List<Pattern<(String) -> (Int) -> ResFuncArg, ResFuncArg, List<Expression>>> =
                paramNames.map { paramName ->
                    map0(anyIndexed(refl(name(equal(paramName)))), paramName)
                }
            val argPat: Pattern<(String) -> (Int) -> ResFuncArg, ResFuncArg, List<Expression>> =
                argNamePatterns.fold(reject()) { acc, elem -> or(acc, elem) }
            val pat = functionCallWithPrefix(
                fprefix = drop(),
                fid = apply(),
                farguments = arguments(
                    fargs = argPat,
                    drop(), drop(), drop()
                )
            )
            parse(pat, onError = null, atom) { funcName ->
                { paramName ->
                    { index ->
                        Pair(paramName, FunctionArg(funcName, index))
                    }
                }
            }?.let {
                addToStorage(it.first, param) { storage -> storage.functionArgs.add(it.second) }
            }
        }

        private fun collectField(atom: Atom, param: MutableMap<String, ArgInfoStorage>) {
            val pat = classField(
                fname = namePat<(String) -> ResField, Name>(),
                fattributeId = apply()
            )
            parse(pat, onError = null, atom) { paramName ->
                { attributeId ->
                    Pair(paramName, Field(attributeId))
                }
            }?.let {
                addToStorage(it.first, param) { storage -> storage.fields.add(it.second) }
            }
        }

        private fun <A> subscriptPat(): Pattern<(String) -> A, A, Atom> =
            atom(
                fatomElement = namePat(),
                ftrailers = first(refl(index(drop())))
            )

        private fun collectAtomMethod(atom: Atom, param: MutableMap<String, ArgInfoStorage>) {
            val methodPat = classMethod(
                fname = namePat<(String) -> ResMethod, Name>(),
                fattributeId = apply(),
                farguments = drop()
            )
            val getPat = swap(map0(subscriptPat<ResMethod>(), "__getitem__"))
            val pat = or(methodPat, getPat)
            parse(pat, onError = null, atom) { paramName ->
                { attributeId ->
                    Pair(paramName, Method(attributeId))
                }
            }?.let {
                addToStorage(it.first, param) { storage -> storage.methods.add(it.second) }
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

        private fun collectMagicMethodsFromCalls(atom: Atom, param: MutableMap<String, ArgInfoStorage>) {
            val callNamePat: Pattern<(String) -> (String) -> ResMethod, (String) -> ResMethod, Name> =
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
            parse(pat, onError = null, atom) { methodName ->
                { paramName ->
                    Pair(paramName, Method(methodName))
                }
            }?.let {
                addToStorage(it.first, param) { storage -> storage.methods.add(it.second) }
            }
        }

        private fun collectFunction(atom: Atom) {
            parse(
                functionCallWithPrefix(
                    fid = apply(),
                    fprefix = drop(),
                    farguments = drop()
                ),
                onError = null,
                atom
            ) { it }?.let { generalStorage.functions.add(Function(it)) }
        }

        private fun collectGeneralMethod(atom: Atom) {
            parse(
                methodFromAtom(
                    fattributeId = apply(),
                    farguments = drop()
                ),
                onError = null,
                atom
            ) { it }?.let { generalStorage.methods.add(Method(it)) }
        }

        private fun collectGeneralFields(atom: Atom) {
            parse(
                attributesFromAtom(fattributes = apply()),
                onError = null,
                atom
            ) { it }?.let { attributes ->
                attributes.forEach { generalStorage.fields.add(Field(it)) }
            }
        }

        override fun visitAtom(atom: Atom, param: MutableMap<String, ArgInfoStorage>): AST {
            collectFunctionArg(atom, param)
            collectField(atom, param)
            collectAtomMethod(atom, param)
            collectMagicMethodsFromCalls(atom, param)
            collectFunction(atom)
            collectGeneralMethod(atom)
            collectGeneralFields(atom)
            return super.visitAtom(atom, param)
        }

        private fun collectTypes(assign: Assign, param: MutableMap<String, ArgInfoStorage>) {
            val pat: Pattern<(String) -> (Type) -> ResAssign, List<ResAssign>, Assign> = assignAll(
                ftargets = allMatches(namePat()), fvalue = typedExpressionPat()
            )
            parse(
                pat,
                onError = emptyList(),
                assign
            ) { paramName -> { typeStorage -> Pair(paramName, typeStorage) } }.map {
                addToStorage(it.first, param) { storage -> storage.types.add(it.second) }
            }
        }

        private fun collectSetItem(assign: Assign, param: MutableMap<String, ArgInfoStorage>) {
            val setItemPat: Pattern<(String) -> String, List<String>, Assign> = assignAll(
                ftargets = allMatches(refl(subscriptPat())),
                fvalue = drop()
            )
            val setItemParams = parse(setItemPat, onError = emptyList(), assign) { it }
            setItemParams.map { paramName ->
                addToStorage(paramName, param) { storage ->
                    storage.methods.add(Method("__setitem__"))
                }
            }
        }

        private fun <A, N> funcCallNamePat(): Pattern<(FunctionRet) -> A, A, N> =
            map1(
                refl(
                    functionCallWithPrefix(
                        fprefix = drop(),
                        fid = apply(),
                        farguments = drop()
                    )
                )
            ) { x -> FunctionRet(x) }

        private fun collectFuncRet(assign: Assign, param: MutableMap<String, ArgInfoStorage>) {
            val pat: Pattern<(String) -> (FunctionRet) -> ResFuncRet, List<ResFuncRet>, Assign> = assignAll(
                ftargets = allMatches(namePat()),
                fvalue = funcCallNamePat()
            )
            val functionRets = parse(pat, onError = emptyList(), assign) { paramName ->
                { functionStorage -> Pair(paramName, functionStorage) }
            }
            functionRets.forEach {
                if (it != null)
                    addToStorage(it.first, param) { storage -> storage.functionRets.add(it.second) }
            }
        }

        override fun visitAssign(ast: Assign, param: MutableMap<String, ArgInfoStorage>): AST {
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

        override fun visitAugAssign(ast: AugAssign, param: MutableMap<String, ArgInfoStorage>): AST {
            parseAndPutType(param, augAssign(ftarget = namePat(), fvalue = typedExpressionPat(), fop = drop()), ast)
            parseAndPutFunctionRet(param, augAssign(ftarget = namePat(), fvalue = funcCallNamePat(), fop = drop()), ast)
            saveToAttributeStorage(ast.target, getOpMagicMethod(ast.op), param)
            saveToAttributeStorage(ast.value, getOpMagicMethod(ast.op), param)
            return super.visitAugAssign(ast, param)
        }

        private fun getOp(ast: BinOp): FuzzedContext =
            when (ast) {
                is Eq -> FuzzedContext.Comparison.EQ
                is NotEq -> FuzzedContext.Comparison.NE
                is Gt -> FuzzedContext.Comparison.GT
                is GtE -> FuzzedContext.Comparison.GE
                is Lt -> FuzzedContext.Comparison.LT
                is LtE -> FuzzedContext.Comparison.LE
                else -> FuzzedContext.Unknown
            }

        private fun getNumFuzzedValue(num: String, op: FuzzedContext = FuzzedContext.Unknown): FuzzedConcreteValue? =
            try {
                when (val x = NumberUtils.createNumber(num)) {
                    is Int -> FuzzedConcreteValue(pythonIntClassId, x.toBigInteger(), op)
                    is Long -> FuzzedConcreteValue(pythonIntClassId, x.toBigInteger(), op)
                    is BigInteger -> FuzzedConcreteValue(pythonIntClassId, x, op)
                    else -> FuzzedConcreteValue(pythonFloatClassId, BigDecimal(num), op)
                }
            } catch (e: NumberFormatException) {
                null
            }

        private fun <A, N> constPat(op: FuzzedContext): Pattern<(FuzzedConcreteValue?) -> A, A, N> {
            val pats = listOf<Pattern<(FuzzedConcreteValue?) -> A, A, N>>(
                map1(refl(num(apply()))) { x -> getNumFuzzedValue(x, op) },
                map1(refl(str(apply()))) { x ->
                    FuzzedConcreteValue(pythonStrClassId, x, op)
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

        override fun visitNum(num: Num, param: MutableMap<String, ArgInfoStorage>): AST {
            val value = getNumFuzzedValue(num.n)
            if (value != null && constStorage.find { it.value == value.value } == null) {
                constStorage.add(value)
                (value.classId as? PythonClassId)?.let { generalStorage.types.add(Type(it)) }
            }
            return super.visitNum(num, param)
        }

        override fun visitStr(str: Str, param: MutableMap<String, ArgInfoStorage>?): AST {
            if (str.s.isEmpty() || str.s[0] != 'f')
                constStorage.add(FuzzedConcreteValue(pythonStrClassId, str.s))
            generalStorage.types.add(Type(pythonStrClassId))
            return super.visitStr(str, param)
        }

        override fun visitBinOp(ast: BinOp, param: MutableMap<String, ArgInfoStorage>): AST {
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
            if (op != FuzzedContext.Unknown)
                parse(
                    binOp(fleft = refl(name(drop())), fright = constPat(op)),
                    onError = null,
                    ast
                ) { it }?.let { constStorage.add(it) }

            saveToAttributeStorage(ast.left, getOpMagicMethod(ast), param)
            saveToAttributeStorage(ast.right, getOpMagicMethod(ast), param)
            return super.visitBinOp(ast, param)
        }

        fun saveToAttributeStorage(name: AST?, methodName: String?, param: MutableMap<String, ArgInfoStorage>) {
            if (methodName == null)
                return
            paramNames.forEach {
                if (name is Name && name.id.name == it) {
                    addToStorage(it, param) { storage ->
                        storage.methods.add(Method(methodName))
                    }
                }
            }
        }

        override fun visitUnaryOp(unaryOp: UnaryOp, param: MutableMap<String, ArgInfoStorage>): AST {
            saveToAttributeStorage(unaryOp.expression, getOpMagicMethod(unaryOp), param)
            return super.visitUnaryOp(unaryOp, param)
        }

        override fun visitDelete(delete: Delete, param: MutableMap<String, ArgInfoStorage>): AST {
            saveToAttributeStorage(delete.expression, "__delitem__", param)
            return super.visitDelete(delete, param)
        }

        override fun visitListExpr(listExpr: ListExpr, param: MutableMap<String, ArgInfoStorage>): AST {
            generalStorage.types.add(Type(PythonListModel.classId))
            return super.visitListExpr(listExpr, param)
        }

        override fun visitSet(
            set: io.github.danielnaczo.python3parser.model.expr.datastructures.Set,
            param: MutableMap<String, ArgInfoStorage>
        ): AST {
            generalStorage.types.add(Type(PythonSetModel.classId))
            return super.visitSet(set, param)
        }

        override fun visitDict(dict: Dict, param: MutableMap<String, ArgInfoStorage>): AST {
            generalStorage.types.add(Type(PythonDictModel.classId))
            return super.visitDict(dict, param)
        }

        override fun visitComprehension(
            comprehension: Comprehension,
            param: MutableMap<String, ArgInfoStorage>
        ): AST {
            generalStorage.methods.add(Method("__iter__"))
            parse(namePat(), onError = null, comprehension.iter) { it }?.let { paramName ->
                addToStorage(paramName, param) { storage -> storage.methods.add(Method("__iter__")) }
            }
            return super.visitComprehension(comprehension, param)
        }

        override fun visitFor(forElement: For, param: MutableMap<String, ArgInfoStorage>): AST {
            generalStorage.methods.add(Method("__iter__"))
            parse(namePat(), onError = null, forElement.iter) { it }?.let { paramName ->
                addToStorage(paramName, param) { storage -> storage.methods.add(Method("__iter__")) }
            }
            return super.visitFor(forElement, param)
        }
    }
}

typealias ResFuncArg = Pair<String, ArgInfoCollector.FunctionArg>?
typealias ResField = Pair<String, ArgInfoCollector.Field>?
typealias ResMethod = Pair<String, ArgInfoCollector.Method>?
typealias ResAssign = Pair<String, ArgInfoCollector.Type>
typealias ResFuncRet = Pair<String, ArgInfoCollector.FunctionRet>?