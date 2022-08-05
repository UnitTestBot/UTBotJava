package org.utbot.python.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider

object UnionModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> {
        var result = emptySequence<FuzzedParameter>()
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val regex = Regex("typing.Union\\[(.*), *(.*)]")
            val annotation = classId.name
            val match = regex.matchEntire(annotation) ?: return@forEach
            parameterIndices.forEach { index ->
                for (newAnnotation in listOf(match.groupValues[1], match.groupValues[2])) {
                    val newDescription = substituteTypesByIndex(
                        description,
                        (0 until description.parameters.size).map {
                            if (it == index) PythonClassId(newAnnotation) else pythonAnyClassId
                        }
                    )
                    result += concreteTypesModelProvider.generate(newDescription)
                }
            }
        }
        return result
    }
}