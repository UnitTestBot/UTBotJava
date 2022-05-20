package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId

/**
 * Method traverser is an object,
 * that helps to collect information about a method.
 *
 * @param name pretty name of the method
 * @param returnType type of returning value
 * @param parameters method parameters types
 * @param concreteValues any concrete values to be processed by fuzzer
 *
 */
class FuzzedMethodDescription(
    val name: String,
    val returnType: ClassId,
    val parameters: List<ClassId>,
    val concreteValues: List<FuzzedConcreteValue> = emptyList()
) {

    /**
     * Map class id to indices of this class in parameters list.
     */
    val parametersMap: Map<ClassId, List<Int>> by lazy {
        val result = mutableMapOf<ClassId, MutableList<Int>>()
        parameters.forEachIndexed { index, classId ->
            result.computeIfAbsent(classId) { mutableListOf() }.add(index)
        }
        result
    }

    constructor(executableId: ExecutableId, concreteValues: List<FuzzedConcreteValue> = emptyList()) : this(
        executableId.name,
        executableId.returnType,
        executableId.parameters,
        concreteValues
    )
}

/**
 * Object to pass concrete values to fuzzer
 */
data class FuzzedConcreteValue(
    val classId: ClassId,
    val value: Any
)