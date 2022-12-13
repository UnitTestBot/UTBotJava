package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.CgTestClassConstructor
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.framework.api.python.*
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.tree.*

class PythonCgVariableConstructor(context_: CgContext) : CgVariableConstructor(context_) {
    private val nameGenerator = CgTestClassConstructor.CgComponents.getNameGeneratorBy(context)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val baseName = name ?: nameGenerator.nameFrom(model.classId)
        return valueByModel.getOrPut(model) {
            when (model) {
                is PythonBoolModel -> CgLiteral(model.classId, model.value)
                is PythonPrimitiveModel -> CgLiteral(model.classId, model.value)
                is PythonTreeModel -> {
                    val (value, children) = pythonBuildObject2(model.tree)
                    CgPythonTree(model.classId, model.tree, value, children)
                }
                is PythonInitObjectModel -> constructInitObjectModel(model, baseName)
                is PythonDictModel -> CgPythonDict(model.stores.map {
                    getOrCreateVariable(it.key) to getOrCreateVariable(
                        it.value
                    )
                }.toMap())

                is PythonListModel -> CgPythonList(model.stores.map { getOrCreateVariable(it) })
                is PythonSetModel -> CgPythonSet(model.stores.map { getOrCreateVariable(it) }.toSet())
                is PythonTupleModel -> CgPythonTuple(model.stores.map { getOrCreateVariable(it) })
                is PythonDefaultModel -> CgPythonRepr(model.classId, model.repr)
                is PythonModel -> error("Unexpected PythonModel: ${model::class}")
                else -> super.getOrCreateVariable(model, name)
            }
        }
    }

    private fun constructInitObjectModel(model: PythonInitObjectModel, baseName: String): CgVariable {
        return newVar(model.classId, baseName) {
            CgConstructorCall(
                ConstructorId(model.classId, model.initValues.map { it.classId }),
                model.initValues.map { getOrCreateVariable(it) }
            )
        }
    }

    fun pythonBuildObject2(objectNode: PythonTree.PythonTreeNode): Pair<CgValue, List<CgStatement>> {
        return when (objectNode) {
            is PythonTree.PrimitiveNode -> {
                Pair(CgLiteral(objectNode.type, objectNode.repr), emptyList())
            }

            is PythonTree.ListNode -> {
                val items = objectNode.items.values.map { pythonBuildObject2(it) }
                Pair(CgPythonList(items.map { it.first }), items.flatMap { it.second })
            }

            is PythonTree.TupleNode -> {
                val items = objectNode.items.values.map { pythonBuildObject2(it) }
                Pair(CgPythonTuple(items.map { it.first }), items.flatMap { it.second })
            }

            is PythonTree.SetNode -> {
                val items = objectNode.items.map { pythonBuildObject2(it) }
                Pair(CgPythonSet(items.map { it.first }.toSet()), items.flatMap { it.second })
            }

            is PythonTree.DictNode -> {
                val keys = objectNode.items.keys.map { pythonBuildObject2(it) }
                val values = objectNode.items.values.map { pythonBuildObject2(it) }
                Pair(CgPythonDict(
                    keys.zip(values).map { (key, value) ->
                        key.first to value.first
                    }.toMap()
                ), keys.flatMap { it.second } + values.flatMap { it.second })
            }

            is PythonTree.ReduceNode -> {
                val id = objectNode.id
                val children = emptyList<CgStatement>().toMutableList()
                if ((context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects.containsKey(id)) {
                    return Pair(
                        (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects[id]!!,
                        children
                    )
                }

                val initArgs = objectNode.args.map {
                    val buildObj = pythonBuildObject2(it)
                    children += buildObj.second
                    buildObj.first
                }
                val constructor = ConstructorId(
                    objectNode.constructor,
                    initArgs.map { it.type }
                )
                val constructorCall = CgConstructorCall(constructor, initArgs)
                val obj = newVar(objectNode.type) {
                    constructorCall
                }
                children.add(CgAssignment(obj, constructorCall))

                (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects[id] = obj

                val state = objectNode.state.map { (key, value) ->
                    val buildObj = pythonBuildObject2(value)
                    children.addAll(buildObj.second)
                    key to buildObj.first
                }.toMap()
                val listitems = objectNode.listitems.map {
                    val buildObj = pythonBuildObject2(it)
                    children.addAll(buildObj.second)
                    buildObj.first
                }
                val dictitems = objectNode.dictitems.map { (key, value) ->
                    val keyObj = pythonBuildObject2(key)
                    val valueObj = pythonBuildObject2(value)
                    children.addAll(keyObj.second)
                    children.addAll(valueObj.second)
                    keyObj.first to valueObj.first
                }

                state.forEach { (key, value) ->
                    val fieldAccess = CgFieldAccess(obj, FieldId(objectNode.type, key))
                    children.add(CgAssignment(fieldAccess, value))
                }
                listitems.forEach {
                    val methodCall = CgMethodCall(
                        obj,
                        PythonMethodId(
                            obj.type as PythonClassId,
                            "append",
                            NormalizedPythonAnnotation(pythonNoneClassId.name),
                            listOf(RawPythonAnnotation(it.type.name))
                        ),
                        listOf(it)
                    )
                    children.add(methodCall)
                }
                dictitems.forEach { (key, value) ->
                    val index = CgPythonIndex(
                        value.type as PythonClassId,
                        obj,
                        key
                    )
                    children.add(CgAssignment(index, value))
                }

                return Pair(obj, children)
            }

            else -> {
                throw UnsupportedOperationException()
            }
        }
    }
}