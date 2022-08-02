package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonInitObjectModel
import org.utbot.fuzzer.*
import org.utbot.python.typing.PythonTypesStorage

object InitModelProvider: ModelProvider {
    private val nonRecursiveModelProvider = ModelProvider.of(DefaultValuesModelProvider, ConstantModelProvider)

    override fun generate(description: FuzzedMethodDescription) = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->

            val type = PythonTypesStorage.getTypeByName(classId) ?: return@forEach
            val constructor = FuzzedMethodDescription(
                type.name,
                classId,
                type.initSignature ?: return@forEach,
                description.concreteValues
            )

            val models = fuzz(constructor, nonRecursiveModelProvider).map { initValues ->
                PythonInitObjectModel(classId.name, initValues.map { it.model })
            }
            parameterIndices.forEach { index ->
                models.forEach { model ->
                    yield(FuzzedParameter(index, model.fuzzed()))
                }
            }
        }
    }
}