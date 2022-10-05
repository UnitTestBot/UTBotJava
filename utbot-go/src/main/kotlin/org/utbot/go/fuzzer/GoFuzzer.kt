package org.utbot.go.fuzzer

import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz
import org.utbot.go.api.GoUtFunction
import org.utbot.go.fuzzer.providers.GoConstantsModelProvider
import org.utbot.go.fuzzer.providers.GoPrimitivesModelProvider
import org.utbot.go.fuzzer.providers.GoStringConstantModelProvider

object GoFuzzer {

    fun goFuzzing(function: GoUtFunction): Sequence<List<FuzzedValue>> {

        /**
         * Unit test generation for functions or methods with no parameters can be useful:
         * one can fixate panic behaviour or its absence.
         */
        if (function.parameters.isEmpty()) {
            return sequenceOf(emptyList())
        }

        // TODO: add more ModelProvider-s
        val modelProviderWithFallback = ModelProvider.of(
            GoConstantsModelProvider,
            GoStringConstantModelProvider,
            GoPrimitivesModelProvider
        )

        return fuzz(function.toFuzzedMethodDescription(), modelProviderWithFallback)
    }

}