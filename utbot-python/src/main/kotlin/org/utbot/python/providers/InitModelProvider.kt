package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonInitObjectModel
import org.utbot.framework.plugin.api.PythonModel
import org.utbot.fuzzer.*
import org.utbot.python.typing.PythonTypesStorage

class InitModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {
    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        if (recursionDepth <= 0)
            return@sequence

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

                    val modelProvider = getDefaultPythonModelProvider(recursionDepth - 1)
                    fuzz(constructor, modelProvider).map { initValues ->
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