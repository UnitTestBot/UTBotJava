package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import java.util.function.IntSupplier

/**
 * Provides different collection for concrete classes.
 *
 * For example, ArrayList, LinkedList, Collections.singletonList can be passed to check
 * if that parameter breaks anything. For example, in case method doesn't expect
 * a non-modifiable collection and tries to add values.
 */
class CollectionModelProvider(
    private val idGenerator: IdGenerator<Int>
) : ModelProvider {

    private val generators = mapOf(
        java.util.List::class.java to ::createListModels,
        java.util.Set::class.java to ::createSetModels,
        java.util.Map::class.java to ::createMapModels,
        java.util.Collection::class.java to ::createCollectionModels,
        java.lang.Iterable::class.java to ::createCollectionModels,
        java.util.Iterator::class.java to ::createIteratorModels,
    )

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .forEach { (classId, indices) ->
                 generators[classId.jClass]?.let { createModels ->
                     yieldAllValues(indices, createModels().map { it.fuzzed() })
                 }
            }
    }

    private fun createListModels(): List<UtAssembleModel> {
        return listOf(
            java.util.List::class.java.createdBy(java.util.ArrayList::class.java.asConstructor()),
            java.util.List::class.java.createdBy(java.util.LinkedList::class.java.asConstructor()),
            java.util.List::class.java.createdBy(java.util.Collections::class.java.methodCall("emptyList", java.util.List::class.java)),
            java.util.List::class.java.createdBy(java.util.Collections::class.java.methodCall("synchronizedList", java.util.List::class.java, params = listOf(java.util.List::class.java)), listOf(
                java.util.List::class.java.createdBy(java.util.ArrayList::class.java.asConstructor())
            )),
        )
    }

    private fun createSetModels(): List<UtAssembleModel> {
        return listOf(
            java.util.Set::class.java.createdBy(java.util.HashSet::class.java.asConstructor()),
            java.util.Set::class.java.createdBy(java.util.TreeSet::class.java.asConstructor()),
            java.util.Set::class.java.createdBy(java.util.Collections::class.java.methodCall("emptySet", java.util.Set::class.java))
        )
    }

    private fun createMapModels(): List<UtAssembleModel> {
        return listOf(
            java.util.Map::class.java.createdBy(java.util.HashMap::class.java.asConstructor()),
            java.util.Map::class.java.createdBy(java.util.TreeMap::class.java.asConstructor()),
            java.util.Map::class.java.createdBy(java.util.Collections::class.java.methodCall("emptyMap", java.util.Map::class.java)),
        )
    }

    private fun createCollectionModels(): List<UtAssembleModel> {
        return listOf(
            java.util.Collection::class.java.createdBy(java.util.ArrayList::class.java.asConstructor()),
            java.util.Collection::class.java.createdBy(java.util.HashSet::class.java.asConstructor()),
            java.util.Collection::class.java.createdBy(java.util.Collections::class.java.methodCall("emptySet", java.util.Set::class.java)),
        )
    }

    private fun createIteratorModels(): List<UtAssembleModel> {
        return listOf(
            java.util.Iterator::class.java.createdBy(java.util.Collections::class.java.methodCall("emptyIterator", java.util.Iterator::class.java)),
        )
    }

    private fun Class<*>.asConstructor() = ConstructorId(id, emptyList())

    private fun Class<*>.methodCall(methodName: String, returnType: Class<*>, params: List<Class<*>> = emptyList()) = MethodId(id, methodName, returnType.id, params.map { it.id })

    private fun Class<*>.createdBy(init: ExecutableId, params: List<UtModel> = emptyList()): UtAssembleModel {
        val instantiationChain = mutableListOf<UtStatementModel>()
        val genId = idGenerator.createId()
        return UtAssembleModel(
            genId,
            id,
            "${init.classId.name}${init.parameters}#" + genId.toString(16),
            instantiationChain
        ).apply {
            instantiationChain += UtExecutableCallModel(null, init, params, this)
        }
    }
}