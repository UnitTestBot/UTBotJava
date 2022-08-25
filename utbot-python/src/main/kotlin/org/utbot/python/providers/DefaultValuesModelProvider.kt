package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonDefaultModel
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.python.typing.PythonTypesStorage

class DefaultValuesModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {
    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        val generated = Array(description.parameters.size) { 0 }
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val pythonClassIdInfo = PythonTypesStorage.findPythonClassIdInfoByName(classId.name) ?: return@forEach
            pythonClassIdInfo.preprocessedInstances?.forEach { instance ->
                parameterIndices.forEach { index ->
                    generated[index] += 1
                    if (generated[index] < 10)
                        yield(FuzzedParameter(
                            index,
                            PythonDefaultModel(instance, pythonClassIdInfo.pythonClassId).fuzzed()
                        ))
                }
            }
        }
    }
}