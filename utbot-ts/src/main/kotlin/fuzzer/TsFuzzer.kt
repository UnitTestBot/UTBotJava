package fuzzer

import fuzzer.providers.TsConstantsModelProvider
import fuzzer.providers.TsObjectModelProvider
import fuzzer.providers.TsPrimitivesModelProvider
import fuzzer.providers.TsStringModelProvider
import fuzzer.providers.TsUndefinedModelProvider
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
