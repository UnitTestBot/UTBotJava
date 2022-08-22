package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

/**
 * Produces bound values for primitive types.
 */
object PrimitivesModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val primitives: List<FuzzedValue> = when (classId) {
                booleanClassId -> listOf(
                    UtPrimitiveModel(false).fuzzed { summary = "%var% = false" },
                    UtPrimitiveModel(true).fuzzed { summary = "%var% = true" }
                )
                charClassId -> listOf(
                    UtPrimitiveModel(Char.MIN_VALUE).fuzzed { summary = "%var% = Char.MIN_VALUE" },
                    UtPrimitiveModel(Char.MAX_VALUE).fuzzed { summary = "%var% = Char.MAX_VALUE" },
                )
                byteClassId -> listOf(
                    UtPrimitiveModel(0.toByte()).fuzzed { summary = "%var% = 0" },
                    UtPrimitiveModel(1.toByte()).fuzzed { summary = "%var% > 0" },
                    UtPrimitiveModel((-1).toByte()).fuzzed { summary = "%var% < 0" },
                    UtPrimitiveModel(Byte.MIN_VALUE).fuzzed { summary = "%var% = Byte.MIN_VALUE" },
                    UtPrimitiveModel(Byte.MAX_VALUE).fuzzed { summary = "%var% = Byte.MAX_VALUE" },
                )
                shortClassId -> listOf(
                    UtPrimitiveModel(0.toShort()).fuzzed { summary = "%var% = 0" },
                    UtPrimitiveModel(1.toShort()).fuzzed { summary = "%var% > 0" },
                    UtPrimitiveModel((-1).toShort()).fuzzed { summary = "%var% < 0" },
                    UtPrimitiveModel(Short.MIN_VALUE).fuzzed { summary = "%var% = Short.MIN_VALUE" },
                    UtPrimitiveModel(Short.MAX_VALUE).fuzzed { summary = "%var% = Short.MAX_VALUE" },
                )
                intClassId -> listOf(
                    UtPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
                    UtPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
                    UtPrimitiveModel((-1)).fuzzed { summary = "%var% < 0" },
                    UtPrimitiveModel(Int.MIN_VALUE).fuzzed { summary = "%var% = Int.MIN_VALUE" },
                    UtPrimitiveModel(Int.MAX_VALUE).fuzzed { summary = "%var% = Int.MAX_VALUE" },
                )
                longClassId -> listOf(
                    UtPrimitiveModel(0L).fuzzed { summary = "%var% = 0L" },
                    UtPrimitiveModel(1L).fuzzed { summary = "%var% > 0L" },
                    UtPrimitiveModel(-1L).fuzzed { summary = "%var% < 0L" },
                    UtPrimitiveModel(Long.MIN_VALUE).fuzzed { summary = "%var% = Long.MIN_VALUE" },
                    UtPrimitiveModel(Long.MAX_VALUE).fuzzed { summary = "%var% = Long.MAX_VALUE" },
                )
                floatClassId -> listOf(
                    UtPrimitiveModel(0.0f).fuzzed { summary = "%var% = 0f" },
                    UtPrimitiveModel(1.1f).fuzzed { summary = "%var% > 0f" },
                    UtPrimitiveModel(-1.1f).fuzzed { summary = "%var% < 0f" },
                    UtPrimitiveModel(Float.MIN_VALUE).fuzzed { summary = "%var% = Float.MIN_VALUE" },
                    UtPrimitiveModel(Float.MAX_VALUE).fuzzed { summary = "%var% = Float.MAX_VALUE" },
                    UtPrimitiveModel(Float.NEGATIVE_INFINITY).fuzzed { summary = "%var% = Float.NEGATIVE_INFINITY" },
                    UtPrimitiveModel(Float.POSITIVE_INFINITY).fuzzed { summary = "%var% = Float.POSITIVE_INFINITY" },
                    UtPrimitiveModel(Float.NaN).fuzzed { summary = "%var% = Float.NaN" },
                )
                doubleClassId -> listOf(
                    UtPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
                    UtPrimitiveModel(1.1).fuzzed { summary = "%var% > 0.0" },
                    UtPrimitiveModel(-1.1).fuzzed { summary = "%var% < 0.0" },
                    UtPrimitiveModel(Double.MIN_VALUE).fuzzed { summary = "%var% = Double.MIN_VALUE" },
                    UtPrimitiveModel(Double.MAX_VALUE).fuzzed { summary = "%var% = Double.MAX_VALUE" },
                    UtPrimitiveModel(Double.NEGATIVE_INFINITY).fuzzed { summary = "%var% = Double.NEGATIVE_INFINITY" },
                    UtPrimitiveModel(Double.POSITIVE_INFINITY).fuzzed { summary = "%var% = Double.POSITIVE_INFINITY" },
                    UtPrimitiveModel(Double.NaN).fuzzed { summary = "%var% = Double.NaN" },
                )
                stringClassId -> listOf(
                    UtPrimitiveModel("").fuzzed { summary = "%var% = empty string" },
                    UtPrimitiveModel("   ").fuzzed { summary = "%var% = blank string" },
                    UtPrimitiveModel("string").fuzzed { summary = "%var% != empty string" },
                    UtPrimitiveModel("\n\t\r").fuzzed { summary = "%var% has special characters" },
                )
                else -> listOf()
            }

            primitives.forEach { model ->
                parameterIndices.forEach { index ->
                    yieldValue(index, model)
                }
            }
        }
    }
}