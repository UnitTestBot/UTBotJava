package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonDefaultModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.python.types.PythonTypesStorage
import java.util.function.BiConsumer

object DefaultValuesModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            PythonTypesStorage.typeNameMap[classId.name]?.let { pythonType ->
                pythonType.instances.forEach { instance ->
                    parameterIndices.forEach { index ->
                        consumer.accept(index, PythonDefaultModel(instance, pythonType.name).fuzzed())
                    }
                }
            }
        }
    }
}