package org.utbot.python.providers

import org.utbot.framework.plugin.api.python.PythonInitObjectModel
import org.utbot.framework.plugin.api.python.PythonModel
import org.utbot.fuzzer.*
import org.utbot.fuzzer.types.ClassIdWrapper
import org.utbot.fuzzer.types.WithClassId
import org.utbot.python.typing.PythonTypesStorage

class InitModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {
    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        if (recursionDepth <= 0)
            return@sequence

        description.parametersMap.forEach { (classId, parameterIndices) ->
            val type = PythonTypesStorage.findPythonClassIdInfoByName((classId as WithClassId).classId.name) ?: return@forEach
            val initSignature = type.initSignature ?: return@forEach

            val models: Sequence<PythonModel> =
                if (initSignature.isEmpty())
                    sequenceOf(PythonInitObjectModel((classId as WithClassId).classId.name, emptyList()))
                else {
                    val constructor = FuzzedMethodDescription(
                        type.pythonClassId.name,
                        classId,
                        initSignature.map { ClassIdWrapper(it) },
                        description.concreteValues
                    )

                    val modelProvider = getDefaultPythonModelProvider(recursionDepth - 1)
                    fuzz(constructor, modelProvider).map { initValues ->
                        PythonInitObjectModel((classId as WithClassId).classId.name, initValues.mapNotNull { it.model as? PythonModel })
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