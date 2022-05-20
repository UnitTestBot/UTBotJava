package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import java.util.function.BiConsumer

/**
 * Creates models of primitives from the method parameters list.
 */
object PrimitivesModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val primitives = when (classId) {
                byteClassId -> listOf(
                    UtPrimitiveModel(1.toByte()),
                    UtPrimitiveModel((-1).toByte()),
                    UtPrimitiveModel(Byte.MAX_VALUE),
                    UtPrimitiveModel(Byte.MIN_VALUE),
                    UtPrimitiveModel(0.toByte()),
                )
                booleanClassId -> listOf(UtPrimitiveModel(false), UtPrimitiveModel(true))
                charClassId -> listOf(
                    UtPrimitiveModel('\\'),
                    UtPrimitiveModel('\t'),
                    UtPrimitiveModel('\n'),
                    UtPrimitiveModel('\u0000'),
                )
                shortClassId -> listOf(
                    UtPrimitiveModel(1.toShort()),
                    UtPrimitiveModel((-1).toShort()),
                    UtPrimitiveModel(Short.MIN_VALUE),
                    UtPrimitiveModel(Short.MAX_VALUE),
                    UtPrimitiveModel(0.toShort()),
                )
                intClassId -> listOf(
                    UtPrimitiveModel(1),
                    UtPrimitiveModel((-1)),
                    UtPrimitiveModel(Int.MIN_VALUE),
                    UtPrimitiveModel(Int.MAX_VALUE),
                    UtPrimitiveModel(0),
                )
                longClassId -> listOf(
                    UtPrimitiveModel(1L),
                    UtPrimitiveModel(-1L),
                    UtPrimitiveModel(Long.MIN_VALUE),
                    UtPrimitiveModel(Long.MAX_VALUE),
                    UtPrimitiveModel(0L),
                )
                floatClassId -> listOf(
                    UtPrimitiveModel(1.1f),
                    UtPrimitiveModel(-1.1f),
                    UtPrimitiveModel(Float.POSITIVE_INFINITY),
                    UtPrimitiveModel(Float.NEGATIVE_INFINITY),
                    UtPrimitiveModel(Float.MIN_VALUE),
                    UtPrimitiveModel(Float.MAX_VALUE),
                    UtPrimitiveModel(Float.NaN),
                    UtPrimitiveModel(0.0f),
                )
                doubleClassId -> listOf(
                    UtPrimitiveModel(1.1),
                    UtPrimitiveModel(-1.1),
                    UtPrimitiveModel(Double.POSITIVE_INFINITY),
                    UtPrimitiveModel(Double.NEGATIVE_INFINITY),
                    UtPrimitiveModel(Double.MIN_VALUE),
                    UtPrimitiveModel(Double.MAX_VALUE),
                    UtPrimitiveModel(Double.NaN),
                    UtPrimitiveModel(0.0),
                )
                stringClassId -> listOf(
                    UtPrimitiveModel(""),
                    UtPrimitiveModel(" "),
                    UtPrimitiveModel("nonemptystring"),
                    UtPrimitiveModel("multiline\n\rstring"),
                    UtPrimitiveModel("1"),
                    UtPrimitiveModel("False"),
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