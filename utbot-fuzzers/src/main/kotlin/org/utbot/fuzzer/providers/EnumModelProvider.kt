package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.isEnum
import org.utbot.framework.plugin.api.reflection
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues

class EnumModelProvider(private val idGenerator: IdentityPreservingIdGenerator<Int>) : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = with(reflection) {
        sequence {
            description.parametersMap
                .asSequence()
                .filter { (classId, _) -> classId.isEnum }
                .forEach { (classId, indices) ->
                    yieldAllValues(indices, classId.javaClass.enumConstants.filterIsInstance<Enum<*>>().map {
                        val id = idGenerator.getOrCreateIdForValue(it)
                        UtEnumConstantModel(id, classId, it).fuzzed { summary = "%var% = ${it.name}" }
                    })
                }
        }
    }
}
