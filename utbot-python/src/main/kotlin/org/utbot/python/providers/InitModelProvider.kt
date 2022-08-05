package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonInitObjectModel
import org.utbot.framework.plugin.api.PythonModel
import org.utbot.fuzzer.*
import org.utbot.python.typing.PythonTypesStorage

object InitModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription) = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->

            val type = PythonTypesStorage.getTypeByName(classId) ?: return@forEach
            val initSignature = type.initSignature ?: return@forEach

            val models: Sequence<PythonModel> =
                if (initSignature.isEmpty())
                    sequenceOf(PythonInitObjectModel(classId.name, emptyList()))
                else {
                    val constructor = FuzzedMethodDescription(
                        type.name,
                        classId,
                        initSignature,
                        description.concreteValues
                    )

                    fuzz(constructor, nonRecursiveModelProvider).map { initValues ->
                        PythonInitObjectModel(classId.name, initValues.map { it.model })
                    }
                }

            parameterIndices.forEach { index ->
                models.forEach { model ->
                    yield(FuzzedParameter(index, model.fuzzed()))
                }
            }
        }
    }
}