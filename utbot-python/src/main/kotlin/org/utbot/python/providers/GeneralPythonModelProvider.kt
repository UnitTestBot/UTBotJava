package org.utbot.python.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.python.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.python.pythonAnyClassId
import org.utbot.fuzzer.*
import org.utbot.fuzzer.types.ClassIdWrapper
import org.utbot.fuzzer.types.Type
import org.utbot.fuzzer.types.WithClassId

val defaultPythonModelProvider = getDefaultPythonModelProvider(recursionDepth = 4)

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
                description.parameters.map { ((it as WithClassId).classId as? NormalizedPythonAnnotation) ?: pythonAnyClassId },
                description.concreteValues
            )
        )

    abstract fun generate(description: PythonFuzzedMethodDescription): Sequence<FuzzedParameter>
}

class PythonFuzzedMethodDescription(
    name: String,
    returnType: Type,
    parameters: List<NormalizedPythonAnnotation>,
    concreteValues: Collection<FuzzedConcreteValue> = emptyList()
): FuzzedMethodDescription(name, returnType, parameters.map { ClassIdWrapper(it) }, concreteValues)

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