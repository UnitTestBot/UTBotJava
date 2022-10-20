package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.types.WithClassId

class EnumModelProvider(private val idGenerator: IdentityPreservingIdGenerator<Int>) : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .filter { (type, _) -> type is WithClassId && type.classId.jClass.isEnum }
            .forEach { (type, indices) ->
                val classId = (type as WithClassId).classId
                val jClass = classId.jClass
                yieldAllValues(indices, jClass.enumConstants.filterIsInstance<Enum<*>>().map {
                    val id = idGenerator.getOrCreateIdForValue(it)
                    UtEnumConstantModel(id, classId, it).fuzzed { summary = "%var% = ${it.name}" }
                })
            }
    }
}
