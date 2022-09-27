package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.objects.create

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
        Info(List::class.id, "emptyList"),
        Info(Set::class.id, "emptySet"),
        Info(Map::class.id, "emptyMap"),
        Info(Collection::class.id, "emptyList", returnType = List::class.id),
        Info(Iterable::class.id, "emptyList", returnType = List::class.id),
        Info(Iterator::class.id, "emptyIterator"),
    )

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .forEach { (classId, indices) ->
                generators.find { classId == it.classId }?.let { generator ->
                    yieldAllValues(indices, listOf(generator.classId.create {
                        id = { idGenerator.createId() }
                        using static method(java.util.Collections::class.id, generator.methodName, returns = generator.returnType) with values()
                    }.fuzzed { summary = "%var% = empty collection" }))
                }
            }
    }

    private class Info(val classId: ClassId, val methodName: String, val returnType: ClassId = classId)
}