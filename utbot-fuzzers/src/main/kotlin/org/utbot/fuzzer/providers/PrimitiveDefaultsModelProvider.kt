package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import java.util.function.BiConsumer

/**
 * Provides default values for primitive types.
 */
object PrimitiveDefaultsModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            valueOf(classId)?.let { model ->
                parameterIndices.forEach { index ->
                    consumer.accept(index, model)
                }
            }
        }
    }

    fun valueOf(classId: ClassId): UtPrimitiveModel? = when (classId) {
        booleanClassId -> UtPrimitiveModel(false)
        byteClassId -> UtPrimitiveModel(0.toByte())
        charClassId -> UtPrimitiveModel('\u0000')
        shortClassId -> UtPrimitiveModel(0.toShort())
        intClassId -> UtPrimitiveModel(0)
        longClassId -> UtPrimitiveModel(0L)
        floatClassId -> UtPrimitiveModel(0.0f)
        doubleClassId -> UtPrimitiveModel(0.0)
        stringClassId -> UtPrimitiveModel("")
        else -> null
    }
}