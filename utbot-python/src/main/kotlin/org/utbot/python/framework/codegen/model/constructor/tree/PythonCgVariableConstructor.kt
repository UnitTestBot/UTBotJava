package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.framework.api.python.*
import org.utbot.python.framework.api.python.util.comparePythonTree
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.constructor.util.dropBuiltins
import org.utbot.python.framework.codegen.model.tree.*

class PythonCgVariableConstructor(cgContext: CgContext) : CgVariableConstructor(cgContext) {
    private val nameGenerator = CgComponents.getNameGeneratorBy(context)
    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val baseName = name ?: nameGenerator.nameFrom(model.classId)

        return valueByUtModelWrapper.getOrPut(model.wrap()) {
            when (model) {
                is PythonTreeModel -> {
                    val (value, arguments) = pythonBuildObject(model.tree, baseName)
                    CgPythonTree(model.classId, model.tree, value, arguments)
                }
                is PythonModel -> error("Unexpected PythonModel: ${model::class}")
                else -> super.getOrCreateVariable(model, name)
            }
        }
    }

    private fun pythonBuildObject(objectNode: PythonTree.PythonTreeNode, baseName: String? = null): Pair<CgValue, List<CgStatement>> {
        return when (objectNode) {
            is PythonTree.PrimitiveNode -> {
                Pair(CgPythonRepr(objectNode.type, objectNode.repr), emptyList())
            }

            is PythonTree.ListNode -> {
                val items = objectNode.items.values.map { pythonBuildObject(it) }
                Pair(CgPythonList(items.map {it.first}), items.flatMap { it.second })
            }

            is PythonTree.TupleNode -> {
                val items = objectNode.items.values.map { pythonBuildObject(it) }
                Pair(CgPythonTuple(items.map {it.first}), items.flatMap { it.second })
            }

            is PythonTree.SetNode -> {
                val items = objectNode.items.map { pythonBuildObject(it) }
                Pair(CgPythonSet(items.map {it.first}.toSet()), items.flatMap { it.second })
            }

            is PythonTree.DictNode -> {
                val keys = objectNode.items.keys.map { pythonBuildObject(it) }
                val values = objectNode.items.values.map { pythonBuildObject(it) }
                Pair(
                    CgPythonDict(
                        keys.zip(values).associate { (key, value) ->
                            key.first to value.first
                        }
                    ),
                    keys.flatMap { it.second } + values.flatMap { it.second }
                )
            }

            is PythonTree.ReduceNode -> {
                val id = objectNode.id
                val assistant = (context.cgLanguageAssistant as PythonCgLanguageAssistant)
                if (assistant.memoryObjects.containsKey(id)) {
                    val tree = assistant.memoryObjectsModels[id]
                    val savedObj = assistant.memoryObjects[id]
                    if (tree != null && savedObj != null && comparePythonTree(tree, objectNode)) {
                        return Pair(savedObj, emptyList())
                    }
                }

                val initArgs = objectNode.args.map {
                    getOrCreateVariable(PythonTreeModel(it, it.type))
                }
                val constructor = ConstructorId(
                    objectNode.constructor,
                    initArgs.map { it.type }
                )
                val constructorCall = CgConstructorCall(constructor, initArgs)
                val obj = newVar(objectNode.type, baseName) {
                    constructorCall
                }

                (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects[id] = obj
                (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjectsModels[id] = objectNode

                val state = objectNode.state.map { (key, value) ->
                    key to getOrCreateVariable(PythonTreeModel(value, value.type))
                }.toMap()
                val listitems = objectNode.listitems.map {
                    getOrCreateVariable(PythonTreeModel(it, it.type))
                }
                val dictitems = objectNode.dictitems.map { (key, value) ->
                    val keyObj = getOrCreateVariable(PythonTreeModel(key, key.type))
                    val valueObj = getOrCreateVariable(PythonTreeModel(value, value.type))
                    keyObj to valueObj
                }

                if (objectNode.customState) {
                    val setstate = state["state"]!!
                    val methodCall = CgMethodCall(
                        obj,
                        PythonMethodId(
                            obj.type as PythonClassId,
                            "__setstate__",
                            NormalizedPythonAnnotation(pythonNoneClassId.name),
                            listOf(RawPythonAnnotation(setstate.type.name))
                        ),
                        listOf(setstate)
                    )
                    +methodCall
                } else {
                    state.forEach { (key, value) ->
                        obj[FieldId(objectNode.type, key)] `=` value
                    }
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
                    +methodCall
                }
                dictitems.forEach { (key, value) ->
                    val index = CgPythonIndex(
                        value.type as PythonClassId,
                        obj,
                        key
                    )
                    index `=` value
                }

                return Pair(obj, context.currentBlock.toList())
            }

            else -> {
                throw UnsupportedOperationException()
            }
        }
    }
}