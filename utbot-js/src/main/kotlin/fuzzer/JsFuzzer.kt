package fuzzer

import fuzzer.providers.JsConstantsModelProvider
import fuzzer.providers.JsMultipleTypesModelProvider
import fuzzer.providers.JsObjectModelProvider
import fuzzer.providers.JsPrimitivesModelProvider
import fuzzer.providers.JsStringModelProvider
import fuzzer.providers.JsUndefinedModelProvider
import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.types.Atom
import org.utbot.fuzzer.types.ConcreteType
import org.utbot.fuzzer.types.Type

val jsUndefinedClassId = ConcreteType(Atom("undefined"))
val jsNumberClassId =  ConcreteType(Atom("number"))
val jsBooleanClassId =  ConcreteType(Atom("bool"))
val jsDoubleClassId =  ConcreteType(Atom("double"))
val jsStringClassId =  ConcreteType(Atom("string"))
val jsErrorClassId =  ConcreteType(Atom("error"))


val jsPrimitives = setOf(
    jsNumberClassId,
    jsBooleanClassId,
    jsDoubleClassId,
)

val jsBasic = setOf(
    jsNumberClassId,
    jsBooleanClassId,
    jsDoubleClassId,
    jsUndefinedClassId,
    jsStringClassId,
)

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
