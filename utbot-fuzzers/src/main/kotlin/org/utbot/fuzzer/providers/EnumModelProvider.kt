package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues

object EnumModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId.jClass.isEnum }
            .forEach { (classId, indices) ->
                yieldAllValues(indices, classId.jClass.enumConstants.filterIsInstance<Enum<*>>().map {
                    UtEnumConstantModel(classId, it).fuzzed { summary = "%var% = ${it.name}" }
                })
            }
    }
}