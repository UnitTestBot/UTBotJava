package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.BinOp
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTypesStorage
import java.math.BigInteger

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
        fun <A, N> valuePat(): Parser<(Storage) -> A, A, N> {
            val consts = listOf<Parser<(Storage) -> A, A, N>>(
                map1(refl(num(apply()))) { x ->
                    Storage("int", FuzzedConcreteValue(PythonIntModel.classId, BigInteger(x))) },
                map1(refl(str(apply()))) { x ->
                    Storage(
                        "str",
                        FuzzedConcreteValue(
                            PythonStrModel.classId,
                            x.removeSurrounding("\"", "\"").removeSurrounding("'", "'")
                        )
                    ) },
                map0(refl(true_()),
                    Storage("bool", FuzzedConcreteValue(PythonBoolModel.classId, true))),
                map0(refl(false_()),
                    Storage("bool", FuzzedConcreteValue(PythonBoolModel.classId, false))),
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

        override fun visitBinOp(ast: BinOp, param: MutableMap<String, MutableList<Storage>>): AST {
            parseAndPut(
                param,
                or(
                    binOp(fleft = namePat(), fright = valuePat()),
                    swap(binOp(fleft = valuePat(), fright = namePat()))
                ),
                ast
            )
            return super.visitBinOp(ast, param)
        }
    }
}