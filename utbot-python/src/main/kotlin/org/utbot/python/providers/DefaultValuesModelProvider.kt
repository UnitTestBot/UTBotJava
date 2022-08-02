package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonDefaultModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.python.typing.PythonTypesStorage

object DefaultValuesModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription) = sequence {
        val generated = Array(description.parameters.size) { 0 }
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val type = PythonTypesStorage.getTypeByName(classId) ?: return@forEach
            type.preprocessedInstances?.forEach { instance ->
                parameterIndices.forEach { index ->
                    generated[index] += 1
                    if (generated[index] < 10)
                        yield(FuzzedParameter(index, PythonDefaultModel(instance, type.name).fuzzed()))
                }
            }
        }
    }
}