package org.utbot.python.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider

val concreteTypesModelProvider = ModelProvider.of(ConstantModelProvider, DefaultValuesModelProvider, InitModelProvider, GenericModelProvider)

fun substituteType(description: FuzzedMethodDescription, typeMap: Map<ClassId, ClassId>): FuzzedMethodDescription {
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

fun substituteTypesByIndex(description: FuzzedMethodDescription, newTypes: List<ClassId>): FuzzedMethodDescription {
    return FuzzedMethodDescription(
        description.name,
        description.returnType,
        newTypes,
        description.concreteValues
    )
}