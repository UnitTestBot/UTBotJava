package fuzzer.providers

import framework.api.ts.TsClassId
import framework.api.ts.TsPrimitiveModel
import framework.api.ts.util.tsBooleanClassId
import framework.api.ts.util.tsDoubleClassId
import framework.api.ts.util.tsNumberClassId
import framework.api.ts.util.tsStringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

object TsPrimitivesModelProvider : ModelProvider {

    // TODO SEVERE: research overflows in ts. For now these nums are low not to go beyond Long (will be fixed)
    internal const val MAX_INT = 1024
    internal const val MIN_INT = -1024

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val primitives = matchClassId(classId as TsClassId)
            primitives.forEach { model ->
                parameterIndices.forEach { index ->
                    yieldValue(index, model)
                }
            }
        }
    }

    private fun matchClassId(classId: TsClassId): List<FuzzedValue> {
        val fuzzedValues = when (classId) {
            tsBooleanClassId -> listOf(
                TsPrimitiveModel(false).fuzzed { summary = "%var% = false" },
                TsPrimitiveModel(true).fuzzed { summary = "%var% = true" }
            )

            tsNumberClassId -> listOf(
                TsPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
                TsPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
                TsPrimitiveModel((-1)).fuzzed { summary = "%var% < 0" },
                TsPrimitiveModel(MIN_INT).fuzzed { summary = "%var% = Number.MIN_SAFE_VALUE" },
                TsPrimitiveModel(MAX_INT).fuzzed { summary = "%var% = Number.MAX_SAFE_VALUE" },
            )

            tsDoubleClassId -> listOf(
                TsPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
                TsPrimitiveModel(1.1).fuzzed { summary = "%var% > 0.0" },
                TsPrimitiveModel(-1.1).fuzzed { summary = "%var% < 0.0" },
                TsPrimitiveModel(MIN_INT.toDouble()).fuzzed { summary = "%var% = Number.MIN_SAFE_VALUE" },
                TsPrimitiveModel(MAX_INT.toDouble()).fuzzed { summary = "%var% = Number.MAX_SAFE_VALUE" },
//                TODO SEVERE: Think about such values as they are present in JavaScript.
//                UtPrimitiveModel(Double.NEGATIVE_INFINITY).fuzzed { summary = "%var% = Double.NEGATIVE_INFINITY" },
//                UtPrimitiveModel(Double.POSITIVE_INFINITY).fuzzed { summary = "%var% = Double.POSITIVE_INFINITY" },
//                TsPrimitiveModel(Double.NaN).fuzzed { summary = "%var% = Double.NaN" },
            )

            tsStringClassId -> primitivesForString()
            else -> listOf()
        }
        return fuzzedValues
    }

    private fun primitivesForString() = listOf(
        TsPrimitiveModel("").fuzzed { summary = "%var% = empty string" },
        TsPrimitiveModel("   ").fuzzed { summary = "%var% = blank string" },
        TsPrimitiveModel("string").fuzzed { summary = "%var% != empty string" },
    )
}