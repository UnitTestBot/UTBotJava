package org.utbot.python.newtyping.mypy

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.*

fun readMypyAnnotationStorage(jsonWithAnnotations: String): MypyAnnotationStorage {
    return jsonAdapter.fromJson(jsonWithAnnotations) ?: error("Couldn't parse json with mypy storage")
}

private val moshi = Moshi.Builder()
    .add(annotationAdapter)
    .add(namesAdapter)
    .add(definitionAdapter)
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val jsonAdapter = moshi.adapter(MypyAnnotationStorage::class.java)

class ExpressionTypeFromMypy(
    val startOffset: Long,
    val endOffset: Long,
    val line: Long,
    val type: MypyAnnotation
)

class MypyAnnotationStorage(
    val nodeStorage: Map<String, MypyAnnotationNode>,
    val definitions: Map<String, Map<String, MypyDefinition>>,
    val types: Map<String, List<ExpressionTypeFromMypy>>,
    val names: Map<String, List<Name>>
) {
    private fun initAnnotation(annotation: MypyAnnotation) {
        if (annotation.initialized)
            return
        annotation.storage = this
        annotation.initialized = true
        annotation.args?.forEach { initAnnotation(it) }
    }
    private fun fillArgNames(definition: MypyDefinition) {
        val node = definition.type.node
        if (node is ConcreteAnnotation) {
            node.members.filterIsInstance<FuncDef>().forEach { funcDef ->
                val nodeInfo = nodeStorage[funcDef.type.nodeId]
                if (nodeInfo is FunctionNode && nodeInfo.argNames.contains(null)) {
                    nodeInfo.argNames = nodeInfo.argNames.zip(funcDef.args).map {
                        it.first ?: (it.second as Variable).name
                    }
                }
            }
        }
    }
    val nodeToUtBotType: MutableMap<MypyAnnotationNode, Type> = mutableMapOf()
    fun getUtBotTypeOfNode(node: MypyAnnotationNode): Type {
        //println("entering $node")
        val mem = nodeToUtBotType[node]
        if (mem != null) {
            //println("exiting $node")
            return mem
        }
        val res = node.initializeType()
        nodeToUtBotType[node] = res
        //println("exiting $node")
        return res
    }
    init {
        definitions.values.forEach { defsInModule ->
            defsInModule.forEach {
                initAnnotation(it.value.type)
                fillArgNames(it.value)
            }
        }
        types.values.flatten().forEach {
            initAnnotation(it.type)
        }
        nodeStorage.values.forEach { node ->
            node.storage = this
            node.children.forEach { initAnnotation(it) }
        }
    }
}