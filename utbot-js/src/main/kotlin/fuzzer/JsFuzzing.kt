package fuzzer

import framework.api.js.JsClassId
import fuzzer.providers.ArrayValueProvider
import fuzzer.providers.BoolValueProvider
import fuzzer.providers.MapValueProvider
import fuzzer.providers.NumberValueProvider
import fuzzer.providers.ObjectValueProvider
import fuzzer.providers.SetValueProvider
import fuzzer.providers.StringValueProvider
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.fuzz

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
    val exec: suspend (JsMethodDescription, List<UtModel>) -> JsFeedback
) : Fuzzing<JsClassId, UtModel, JsMethodDescription, JsFeedback> {

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, UtModel>> {
        return defaultValueProviders().asSequence().flatMap { provider ->
            if (provider.accept(type)) {
                provider.generate(description, type)
            } else {
                emptySequence()
            }
        }
    }

    override suspend fun handle(description: JsMethodDescription, values: List<UtModel>): JsFeedback {
        return exec(description, values)
    }
}

suspend fun runFuzzing(
    description: JsMethodDescription,
    exec: suspend (JsMethodDescription, List<UtModel>) -> JsFeedback
) = JsFuzzing(exec).fuzz(description)
