package fuzzer

import framework.api.js.JsClassId
import fuzzer.providers.BoolValueProvider
import fuzzer.providers.MapValueProvider
import fuzzer.providers.NumberValueProvider
import fuzzer.providers.SetValueProvider
import fuzzer.providers.StringValueProvider
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.fuzz
import fuzzer.providers.ArrayValueProvider
import fuzzer.providers.ObjectValueProvider

fun defaultValueProviders() = listOf(
    BoolValueProvider,
    NumberValueProvider,
    StringValueProvider,
    MapValueProvider,
    SetValueProvider,
    ObjectValueProvider(),
    ArrayValueProvider()
)

class JsFuzzing(
    val exec: suspend (JsMethodDescription, List<JsFuzzedValue>) -> JsFeedback
) : Fuzzing<JsClassId, JsFuzzedValue, JsMethodDescription, JsFeedback> {

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, JsFuzzedValue>> {
        return defaultValueProviders().asSequence().flatMap { provider ->
            if (provider.accept(type)) {
                provider.generate(description, type)
            } else {
                emptySequence()
            }
        }
    }

    override suspend fun handle(description: JsMethodDescription, values: List<JsFuzzedValue>): JsFeedback {
        return exec(description, values)
    }
}

suspend fun runFuzzing(
    description: JsMethodDescription,
    exec: suspend (JsMethodDescription, List<JsFuzzedValue>) -> JsFeedback
) = JsFuzzing(exec).fuzz(description)
