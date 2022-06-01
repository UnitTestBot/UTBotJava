package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import java.util.function.BiConsumer

/**
 * Produces bound values for primitive types.
 */
object PrimitivesModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val primitives = when (classId) {
                byteClassId -> listOf(
                    UtPrimitiveModel(0.toByte()),
                    UtPrimitiveModel(1.toByte()),
                    UtPrimitiveModel((-1).toByte()),
                    UtPrimitiveModel(Byte.MIN_VALUE),
                    UtPrimitiveModel(Byte.MAX_VALUE),
                )
                booleanClassId -> listOf(UtPrimitiveModel(false), UtPrimitiveModel(true))
                charClassId -> listOf(
                    UtPrimitiveModel('\\'),
                    UtPrimitiveModel(Char.MIN_VALUE),
                    UtPrimitiveModel(Char.MAX_VALUE),
                )
                shortClassId -> listOf(
                    UtPrimitiveModel(0.toShort()),
                    UtPrimitiveModel(1.toShort()),
                    UtPrimitiveModel((-1).toShort()),
                    UtPrimitiveModel(Short.MIN_VALUE),
                    UtPrimitiveModel(Short.MAX_VALUE),
                )
                intClassId -> listOf(
                    UtPrimitiveModel(1),
                    UtPrimitiveModel((-1)),
                    UtPrimitiveModel(Int.MIN_VALUE),
                    UtPrimitiveModel(Int.MAX_VALUE),
                    UtPrimitiveModel(0),
                )
                longClassId -> listOf(
                    UtPrimitiveModel(0L),
                    UtPrimitiveModel(1L),
                    UtPrimitiveModel(-1L),
                    UtPrimitiveModel(Long.MIN_VALUE),
                    UtPrimitiveModel(Long.MAX_VALUE),
                )
                floatClassId -> listOf(
                    UtPrimitiveModel(0.0f),
                    UtPrimitiveModel(1.1f),
                    UtPrimitiveModel(-1.1f),
                    UtPrimitiveModel(Float.MIN_VALUE),
                    UtPrimitiveModel(Float.MAX_VALUE),
                    UtPrimitiveModel(Float.NEGATIVE_INFINITY),
                    UtPrimitiveModel(Float.POSITIVE_INFINITY),
                    UtPrimitiveModel(Float.NaN),
                )
                doubleClassId -> listOf(
                    UtPrimitiveModel(0.0),
                    UtPrimitiveModel(1.1),
                    UtPrimitiveModel(-1.1),
                    UtPrimitiveModel(Double.MIN_VALUE),
                    UtPrimitiveModel(Double.MAX_VALUE),
                    UtPrimitiveModel(Double.NEGATIVE_INFINITY),
                    UtPrimitiveModel(Double.POSITIVE_INFINITY),
                    UtPrimitiveModel(Double.NaN),
                )
                stringClassId -> listOf(
                    UtPrimitiveModel(""),
                    UtPrimitiveModel("   "),
                    UtPrimitiveModel("string"),
                    UtPrimitiveModel("\n\t\r"),
                )
                else -> listOf()
            }

            primitives.forEach { model ->
                parameterIndices.forEach { index ->
                    consumer.accept(index, model)
                }
            }
        }
    }
}