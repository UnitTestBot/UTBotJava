package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.wrapperByPrimitive
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.consumeAll
import java.util.function.BiConsumer

object PrimitiveWrapperModelProvider: ModelProvider {

    private val constantModels = ModelProvider.of(
        PrimitiveDefaultsModelProvider,
        ConstantsModelProvider,
        StringConstantModelProvider
    )

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
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
            return
        }

        val constants = mutableMapOf<ClassId, MutableList<UtModel>>()
        constantModels.generate(FuzzedMethodDescription(
            name = this::class.simpleName + " constant generation ",
            returnType = voidClassId,
            parameters = primitiveWrapperTypesAsPrimitiveTypes,
            concreteValues = description.concreteValues
        )) { index, value ->
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
                    consumer.consumeAll(indices, models)
                }
            }
    }
}