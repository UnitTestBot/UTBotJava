package org.utbot.python.providers

import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.fuzzer.FuzzedParameter

class UnionModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {
    override fun generate(description: PythonFuzzedMethodDescription): Sequence<FuzzedParameter> {
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
                            if (it == index) NormalizedPythonAnnotation(newAnnotation) else pythonAnyClassId
                        }
                    )
                    result += getDefaultPythonModelProvider(recursionDepth).generate(newDescription)
                }
            }
        }
        return result
    }
}