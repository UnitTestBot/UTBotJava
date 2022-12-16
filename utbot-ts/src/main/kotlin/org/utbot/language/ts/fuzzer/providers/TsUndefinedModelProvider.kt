package org.utbot.language.ts.fuzzer.providers

import org.utbot.language.ts.fuzzer.providers.TsPrimitivesModelProvider.MAX_INT
import org.utbot.language.ts.fuzzer.providers.TsPrimitivesModelProvider.MIN_INT
import org.utbot.language.ts.framework.api.ts.TsPrimitiveModel
import org.utbot.language.ts.framework.api.ts.util.tsUndefinedClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider

object TsUndefinedModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val parameters = description.parametersMap.getOrDefault(tsUndefinedClassId, emptyList())
        val primitives: List<FuzzedValue> = generateValues()
        primitives.forEach { model ->
            parameters.forEach { index ->
                yield(FuzzedParameter(index, model))
            }
        }
    }

    private fun generateValues() =
        listOf(
            TsPrimitiveModel(false).fuzzed { summary = "%var% = false" },
            TsPrimitiveModel(true).fuzzed { summary = "%var% = false" },

            TsPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
            TsPrimitiveModel(-1).fuzzed { summary = "%var% < 0" },
            TsPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
            TsPrimitiveModel(MAX_INT).fuzzed { summary = "%var% = Number.MAX_SAFE_VALUE" },
            TsPrimitiveModel(MIN_INT).fuzzed { summary = "%var% = Number.MIN_SAFE_VALUE" },

            TsPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
            TsPrimitiveModel(-1.0).fuzzed { summary = "%var% < 0.0" },
            TsPrimitiveModel(1.0).fuzzed { summary = "%var% > 0.0" },
        )
}