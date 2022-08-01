package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonDefaultModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import com.beust.klaxon.Klaxon

data class PythonType(
    val name: String,
    val instances: List<String>,
    val useAsReturn: Boolean
)

object PythonTypesStorage {
    val builtinTypes: List<PythonType>
    init {
        val typesAsString = PythonTypesStorage::class.java.getResource("/builtin_types.json")?.readText(Charsets.UTF_8)
            ?: error("Didn't find builtin_types.json")
        builtinTypes =  Klaxon().parseArray(typesAsString) ?: emptyList()
    }

    val typeNameMap: Map<String, PythonType> by lazy {
        val result = mutableMapOf<String, PythonType>()
        builtinTypes.forEach { type ->
            result[type.name] = type
        }
        result
    }
}

object DefaultValuesModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription) = sequence {
        val generated = Array(description.parameters.size) { 0 }
        description.parametersMap.forEach { (classId, parameterIndices) ->
            PythonTypesStorage.typeNameMap[classId.name]?.let { pythonType ->
                pythonType.instances.forEach { instance ->
                    parameterIndices.forEach { index ->
                        generated[index] += 1
                        if (generated[index] < 10)
                            yield(FuzzedParameter(index, PythonDefaultModel(instance, pythonType.name).fuzzed()))
                    }
                }
            }
        }
    }
}