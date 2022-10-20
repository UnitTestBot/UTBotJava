package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.util.id
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.objects.create
import org.utbot.fuzzer.types.Type
import org.utbot.fuzzer.types.WithClassId
import org.utbot.fuzzer.types.toJavaType

/**
 * Provides different collection for concrete classes.
 *
 * For example, ArrayList, LinkedList, Collections.singletonList can be passed to check
 * if that parameter breaks anything. For example, in case method doesn't expect
 * a non-modifiable collection and tries to add values.
 */
class CollectionWithEmptyStatesModelProvider(
    private val idGenerator: IdGenerator<Int>
) : ModelProvider {

    private val generators = listOf(
        Info(List::class.id.toJavaType(), "emptyList"),
        Info(Set::class.id.toJavaType(), "emptySet"),
        Info(Map::class.id.toJavaType(), "emptyMap"),
        Info(Collection::class.id.toJavaType(), "emptyList", returnType = List::class.id.toJavaType()),
        Info(Iterable::class.id.toJavaType(), "emptyList", returnType = List::class.id.toJavaType()),
        Info(Iterator::class.id.toJavaType(), "emptyIterator"),
    )

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .forEach { (classId, indices) ->
                generators.find { classId == it.type }?.let { generator ->
                    yieldAllValues(indices, listOf((generator.type as WithClassId).classId.create {
                        id = { idGenerator.createId() }
                        using static method(java.util.Collections::class.id, generator.methodName, returns = (generator.returnType as WithClassId).classId) with values()
                    }.fuzzed { summary = "%var% = empty collection" }))
                }
            }
    }

    private class Info(val type: Type, val methodName: String, val returnType: Type = type)
}