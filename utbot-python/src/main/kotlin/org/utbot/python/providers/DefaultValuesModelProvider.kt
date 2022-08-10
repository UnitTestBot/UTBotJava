package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.PythonDefaultModel
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.python.typing.PythonTypesStorage

object DefaultValuesModelProvider: PythonModelProvider() {
    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        val generated = Array(description.parameters.size) { 0 }
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val type = PythonTypesStorage.getTypeByName(PythonClassId(classId.name)) ?: return@forEach
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