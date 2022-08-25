package org.utbot.python.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.fuzzer.*

val defaultPythonModelProvider = getDefaultPythonModelProvider(recursionDepth = 5)

fun getDefaultPythonModelProvider(recursionDepth: Int): ModelProvider =
    ModelProvider.of(
        ConstantModelProvider(recursionDepth),
        DefaultValuesModelProvider(recursionDepth),
        GenericModelProvider(recursionDepth),
        UnionModelProvider(recursionDepth),
        OptionalModelProvider(recursionDepth),
        InitModelProvider(recursionDepth)
    )

abstract class PythonModelProvider(protected val recursionDepth: Int): ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> =
        generate(
            PythonFuzzedMethodDescription(
                description.name,
                description.returnType,
                description.parameters.map { (it as? NormalizedPythonAnnotation) ?: pythonAnyClassId },
                description.concreteValues
            )
        )

    abstract fun generate(description: PythonFuzzedMethodDescription): Sequence<FuzzedParameter>
}

class PythonFuzzedMethodDescription(
    name: String,
    returnType: ClassId,
    parameters: List<NormalizedPythonAnnotation>,
    concreteValues: Collection<FuzzedConcreteValue> = emptyList()
): FuzzedMethodDescription(name, returnType, parameters, concreteValues)

fun substituteType(
    description: FuzzedMethodDescription,
    typeMap: Map<PythonClassId, ClassId>
): FuzzedMethodDescription {
    val newReturnType = typeMap[description.returnType] ?: description.returnType
    val newParameters = description.parameters.map { typeMap[it] ?: it }
    val newConcreteValues = description.concreteValues.map { value ->
        val newType = typeMap[value.classId]
        if (newType != null) {
            FuzzedConcreteValue(newType, value.value, value.relativeOp)
        } else {
            value
        }
    }

    return FuzzedMethodDescription(description.name, newReturnType, newParameters, newConcreteValues)
}

fun substituteTypesByIndex(
    description: PythonFuzzedMethodDescription,
    newTypes: List<NormalizedPythonAnnotation>
): PythonFuzzedMethodDescription {
    return PythonFuzzedMethodDescription(
        description.name,
        description.returnType,
        newTypes,
        description.concreteValues
    )
}