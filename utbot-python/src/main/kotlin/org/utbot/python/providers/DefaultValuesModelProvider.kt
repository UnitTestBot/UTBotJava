package org.utbot.python.providers

import org.utbot.fuzzer.FuzzedParameter
import org.utbot.python.framework.api.python.PythonDefaultModel
import org.utbot.python.typing.PythonTypesStorage

const val MAX_DEEP = 10

class DefaultValuesModelProvider(recursionDepth: Int) : PythonModelProvider(recursionDepth) {
    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        val generated = Array(description.parameters.size) { 0 }
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val pythonClassIdInfo = PythonTypesStorage.findPythonClassIdInfoByName(classId.name) ?: return@forEach
            pythonClassIdInfo.preprocessedInstances?.forEach { instance ->
                parameterIndices.forEach { index ->
                    generated[index] += 1
                    if (generated[index] < MAX_DEEP)
                        yield(
                            FuzzedParameter(
                                index,
                                PythonDefaultModel(instance, pythonClassIdInfo.pythonClassId).fuzzed()
                            )
                        )
                }
            }
        }
    }
}