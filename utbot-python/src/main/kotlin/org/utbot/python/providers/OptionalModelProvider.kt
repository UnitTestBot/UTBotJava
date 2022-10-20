package org.utbot.python.providers

import org.utbot.framework.plugin.api.python.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.python.pythonAnyClassId
import org.utbot.framework.plugin.api.python.pythonNoneClassId
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.types.WithClassId

class OptionalModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {
    override fun generate(description: PythonFuzzedMethodDescription): Sequence<FuzzedParameter> {
        var result = emptySequence<FuzzedParameter>()
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val regex = Regex("typing.Optional\\[(.*)]")
            val annotation = (classId as WithClassId).classId.name
            val match = regex.matchEntire(annotation) ?: return@forEach
            parameterIndices.forEach { index ->
                val descriptionWithNoneType = substituteTypesByIndex(
                    description,
                    (0 until description.parameters.size).map {
                        if (it == index) NormalizedPythonAnnotation(pythonNoneClassId.name)  else pythonAnyClassId
                    }
                )
                val modelProvider = getDefaultPythonModelProvider(recursionDepth)
                result += modelProvider.generate(descriptionWithNoneType)
                val descriptionWithNonNoneType = substituteTypesByIndex(
                    description,
                    (0 until description.parameters.size).map {
                        if (it == index) NormalizedPythonAnnotation(match.groupValues[1]) else pythonAnyClassId
                    }
                )
                result += modelProvider.generate(descriptionWithNonNoneType)
            }
        }
        return result
    }
}