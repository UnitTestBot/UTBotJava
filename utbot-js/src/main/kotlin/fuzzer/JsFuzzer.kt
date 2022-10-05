package fuzzer

import fuzzer.providers.JsConstantsModelProvider
import fuzzer.providers.JsMultipleTypesModelProvider
import fuzzer.providers.JsObjectModelProvider
import fuzzer.providers.JsPrimitivesModelProvider
import fuzzer.providers.JsStringModelProvider
import fuzzer.providers.JsUndefinedModelProvider
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz

object JsFuzzer {

    fun jsFuzzing(
        modelProvider: (ModelProvider) -> ModelProvider = { it },
        methodUnderTestDescription: FuzzedMethodDescription
    ): Sequence<List<FuzzedValue>> {
        val modelProviderWithFallback = modelProvider(
            ModelProvider.of(
                JsConstantsModelProvider,
                JsUndefinedModelProvider,
                JsStringModelProvider,
                JsMultipleTypesModelProvider,
                JsPrimitivesModelProvider,
                JsObjectModelProvider,
            )
        )
        return fuzz(methodUnderTestDescription, modelProviderWithFallback)
    }
}
