package fuzzer

import framework.api.js.JsClassId
import fuzzer.providers.BoolValueProvider
import fuzzer.providers.NumberValueProvider
import fuzzer.providers.ObjectValueProvider
import fuzzer.providers.StringValueProvider
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ReferencePreservingIntIdGenerator
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.fuzz


fun defaultValueProviders() = listOf(
    BoolValueProvider,
    NumberValueProvider,
    StringValueProvider,
    ObjectValueProvider()
)

class JsFuzzing(
    val exec: suspend (JsMethodDescription, List<FuzzedValue>) -> JsFeedback
) : Fuzzing<JsClassId, FuzzedValue, JsMethodDescription, JsFeedback> {

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, FuzzedValue>> {
        return defaultValueProviders().asSequence().flatMap { provider ->
            if (provider.accept(type)) {
                provider.generate(description, type)
            } else {
                emptySequence()
            }
        }
    }

    override suspend fun handle(description: JsMethodDescription, values: List<FuzzedValue>): JsFeedback {
        return exec(description, values)
    }
}

internal val defaultFuzzingIdGenerator = ReferencePreservingIntIdGenerator()


suspend fun runFuzzing(
    description: JsMethodDescription,
    exec: suspend (JsMethodDescription, List<FuzzedValue>) -> JsFeedback
) = JsFuzzing(exec).fuzz(description)
