package fuzzer.new

import framework.api.js.JsClassId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.Configuration
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.fuzz
import kotlin.random.Random


fun defaultValueProviders() = listOf(
    BoolValueProvider,
    NumberValueProvider
)

class JsFuzzing(
    val exec: suspend (JsMethodDescription, List<FuzzedValue>) -> Unit
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
        exec(description, values)
        return JsFeedback(values = emptyList())
    }
}

fun runFuzzing(description: JsMethodDescription): Flow<List<FuzzedValue>> = flow {
    JsFuzzing { _, values ->
        emit(values)
    }.fuzz(description, Random(0), Configuration())
}