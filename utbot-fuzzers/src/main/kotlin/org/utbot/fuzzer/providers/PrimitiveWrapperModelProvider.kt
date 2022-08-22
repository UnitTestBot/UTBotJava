package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.wrapperByPrimitive
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues

object PrimitiveWrapperModelProvider: ModelProvider {

    private val constantModels = ModelProvider.of(
        PrimitiveDefaultsModelProvider,
        ConstantsModelProvider,
        StringConstantModelProvider
    )

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val primitiveWrapperTypesAsPrimitiveTypes = description.parametersMap
            .keys
            .asSequence()
            .filter {
                it == stringClassId || it.isPrimitiveWrapper
            }
            .mapNotNull { classId ->
                when {
                    classId == stringClassId -> stringClassId
                    classId.isPrimitiveWrapper -> primitiveByWrapper[classId]
                    else -> null
                }
            }.toList()

        if (primitiveWrapperTypesAsPrimitiveTypes.isEmpty()) {
            return@sequence
        }

        val constants = mutableMapOf<ClassId, MutableList<FuzzedValue>>()
        constantModels.generate(FuzzedMethodDescription(
            name = "Primitive wrapper constant generation ",
            returnType = voidClassId,
            parameters = primitiveWrapperTypesAsPrimitiveTypes,
            concreteValues = description.concreteValues
        )).forEach { (index, value) ->
            val primitiveWrapper = wrapperByPrimitive[primitiveWrapperTypesAsPrimitiveTypes[index]]
            if (primitiveWrapper != null) {
                constants.computeIfAbsent(primitiveWrapper) { mutableListOf() }.add(value)
            }
        }

        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId == stringClassId || classId.isPrimitiveWrapper }
            .forEach { (classId, indices) ->
                constants[classId]?.let { models ->
                    yieldAllValues(indices, models)
                }
            }
    }
}