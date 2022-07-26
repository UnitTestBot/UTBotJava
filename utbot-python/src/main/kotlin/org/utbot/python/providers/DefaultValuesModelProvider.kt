package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonDefaultModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.python.PythonTypesStorage
import java.util.function.BiConsumer

object DefaultValuesModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription) = sequence {
        var generated = 0
        description.parametersMap.forEach { (classId, parameterIndices) ->
            PythonTypesStorage.typeNameMap[classId.name]?.let { pythonType ->
                pythonType.instances.forEach { instance ->
                    parameterIndices.forEach { index ->
                        generated += 1
                        if (generated < 10)
                            yield(FuzzedParameter(index, PythonDefaultModel(instance, pythonType.name).fuzzed()))
                    }
                }
            }
        }
    }
}