package org.utbot.language.ts.fuzzer

import org.utbot.language.ts.fuzzer.providers.TsConstantsModelProvider
import org.utbot.language.ts.fuzzer.providers.TsObjectModelProvider
import org.utbot.language.ts.fuzzer.providers.TsPrimitivesModelProvider
import org.utbot.language.ts.fuzzer.providers.TsStringModelProvider
import org.utbot.language.ts.fuzzer.providers.TsUndefinedModelProvider
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz

object TsFuzzer {

    fun tsFuzzing(
        modelProvider: (ModelProvider) -> ModelProvider = { it },
        methodUnderTestDescription: FuzzedMethodDescription
    ): Sequence<List<FuzzedValue>> {
        val modelProviderWithFallback = modelProvider(
            ModelProvider.of(
                TsConstantsModelProvider,
                TsUndefinedModelProvider,
                TsStringModelProvider,
                TsPrimitivesModelProvider,
                TsObjectModelProvider,
            )
        )
        return fuzz(methodUnderTestDescription, modelProviderWithFallback)
    }
}
