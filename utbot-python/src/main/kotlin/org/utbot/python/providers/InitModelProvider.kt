package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonInitObjectModel
import org.utbot.framework.plugin.api.PythonModel
import org.utbot.fuzzer.*
import org.utbot.python.typing.PythonTypesStorage

object InitModelProvider: PythonModelProvider() {
    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val type = PythonTypesStorage.findPythonClassIdInfoByName(classId.name) ?: return@forEach
            val initSignature = type.initSignature ?: return@forEach

            val models: Sequence<PythonModel> =
                if (initSignature.isEmpty())
                    sequenceOf(PythonInitObjectModel(classId.name, emptyList()))
                else {
                    val constructor = FuzzedMethodDescription(
                        type.pythonClassId.name,
                        classId,
                        initSignature,
                        description.concreteValues
                    )

                    fuzz(constructor, nonRecursiveModelProvider).map { initValues ->
                        PythonInitObjectModel(classId.name, initValues.mapNotNull { it.model as? PythonModel })
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