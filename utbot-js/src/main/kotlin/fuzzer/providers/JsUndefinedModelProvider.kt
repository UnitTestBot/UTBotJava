package fuzzer.providers

import fuzzer.providers.JsPrimitivesModelProvider.MAX_INT
import fuzzer.providers.JsPrimitivesModelProvider.MIN_INT
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.jsUndefinedClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider

object JsUndefinedModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val parameters = description.parametersMap.getOrDefault(jsUndefinedClassId, emptyList())
        val primitives: List<FuzzedValue> = generateValues()
        primitives.forEach { model ->
            parameters.forEach { index ->
                yield(FuzzedParameter(index, model))
            }
        }
    }

    private fun generateValues() =
        listOf(
            JsPrimitiveModel(false).fuzzed { summary = "%var% = false" },
            JsPrimitiveModel(true).fuzzed { summary = "%var% = false" },

            JsPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
            JsPrimitiveModel(-1).fuzzed { summary = "%var% < 0" },
            JsPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
            JsPrimitiveModel(MAX_INT).fuzzed { summary = "%var% = Number.MAX_SAFE_VALUE" },
            JsPrimitiveModel(MIN_INT).fuzzed { summary = "%var% = Number.MIN_SAFE_VALUE" },

            JsPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
            JsPrimitiveModel(-1.0).fuzzed { summary = "%var% < 0.0" },
            JsPrimitiveModel(1.0).fuzzed { summary = "%var% > 0.0" },
        )
}