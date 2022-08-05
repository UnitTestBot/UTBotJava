package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.framework.plugin.api.pythonNoneClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider

object OptionalModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> {
        var result = emptySequence<FuzzedParameter>()
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val regex = Regex("typing.Optional\\[(.*)]")
            val annotation = classId.name
            val match = regex.matchEntire(annotation) ?: return@forEach
            parameterIndices.forEach { index ->
                val descriptionWithNoneType = substituteTypesByIndex(
                    description,
                    (0 until description.parameters.size).map {
                        if (it == index) pythonNoneClassId else pythonAnyClassId
                    }
                )
                result += concreteTypesModelProvider.generate(descriptionWithNoneType)
                val descriptionWithNonNoneType = substituteTypesByIndex(
                    description,
                    (0 until description.parameters.size).map {
                        if (it == index) PythonClassId(match.groupValues[1]) else pythonAnyClassId
                    }
                )
                result += concreteTypesModelProvider.generate(descriptionWithNonNoneType)
            }
        }
        return result
    }
}