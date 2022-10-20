package fuzzer.providers

import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.framework.plugin.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.js.util.jsBooleanClassId
import org.utbot.framework.plugin.api.js.util.jsDoubleClassId
import org.utbot.framework.plugin.api.js.util.jsNumberClassId
import org.utbot.framework.plugin.api.js.util.jsStringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.types.WithClassId

object JsPrimitivesModelProvider : ModelProvider {

    // TODO SEVERE: research overflows in js. For now these nums are low not to go beyond Long (will be fixed)
    internal const val MAX_INT = 1024
    internal const val MIN_INT = -1024

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val primitives = matchClassId((classId as WithClassId).classId as JsClassId)
            primitives.forEach { model ->
                parameterIndices.forEach { index ->
                    yieldValue(index, model)
                }
            }
        }
    }

    internal fun matchClassId(classId: JsClassId): List<FuzzedValue> {
        val fuzzedValues = when (classId) {
            jsBooleanClassId -> listOf(
                JsPrimitiveModel(false).fuzzed { summary = "%var% = false" },
                JsPrimitiveModel(true).fuzzed { summary = "%var% = true" }
            )

            jsNumberClassId -> listOf(
                JsPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
                JsPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
                JsPrimitiveModel((-1)).fuzzed { summary = "%var% < 0" },
                JsPrimitiveModel(MIN_INT).fuzzed { summary = "%var% = Number.MIN_SAFE_VALUE" },
                JsPrimitiveModel(MAX_INT).fuzzed { summary = "%var% = Number.MAX_SAFE_VALUE" },
            )

            jsDoubleClassId -> listOf(
                JsPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
                JsPrimitiveModel(1.1).fuzzed { summary = "%var% > 0.0" },
                JsPrimitiveModel(-1.1).fuzzed { summary = "%var% < 0.0" },
                JsPrimitiveModel(MIN_INT.toDouble()).fuzzed { summary = "%var% = Number.MIN_SAFE_VALUE" },
                JsPrimitiveModel(MAX_INT.toDouble()).fuzzed { summary = "%var% = Number.MAX_SAFE_VALUE" },
//                TODO SEVERE: Think about such values as they are present in JavaScript.
//                UtPrimitiveModel(Double.NEGATIVE_INFINITY).fuzzed { summary = "%var% = Double.NEGATIVE_INFINITY" },
//                UtPrimitiveModel(Double.POSITIVE_INFINITY).fuzzed { summary = "%var% = Double.POSITIVE_INFINITY" },
//                JsPrimitiveModel(Double.NaN).fuzzed { summary = "%var% = Double.NaN" },
            )

            jsStringClassId -> primitivesForString()
            else -> listOf()
        }
        return fuzzedValues
    }

    internal fun primitivesForString() = listOf(
        JsPrimitiveModel("").fuzzed { summary = "%var% = empty string" },
        JsPrimitiveModel("   ").fuzzed { summary = "%var% = blank string" },
        JsPrimitiveModel("string").fuzzed { summary = "%var% != empty string" },
    )
}