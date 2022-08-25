package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

object StringConstantModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId == stringClassId }
            .forEach { (_, value, _) ->
                description.parametersMap.getOrElse(stringClassId) { emptyList() }.forEach { index ->
                    yieldValue(index, UtPrimitiveModel(value).fuzzed { summary = "%var% = string" })
                }
            }
        val charsAsStrings = description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId == charClassId }
            .map { (_, value, _) ->
                UtPrimitiveModel((value as Char).toString()).fuzzed {
                    summary = "%var% = $value"
                }
            }
        yieldAllValues(description.parametersMap.getOrElse(stringClassId) { emptyList() }, charsAsStrings)
    }
}