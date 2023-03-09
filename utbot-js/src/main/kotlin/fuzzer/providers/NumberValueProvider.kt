package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import fuzzer.JsFuzzedContext.EQ
import fuzzer.JsFuzzedContext.GE
import fuzzer.JsFuzzedContext.GT
import fuzzer.JsFuzzedContext.LE
import fuzzer.JsFuzzedContext.LT
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.DefaultFloatBound
import org.utbot.fuzzing.seeds.IEEE754Value

object NumberValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic
    }

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, UtModel>> =
        sequence {
            description.concreteValues.forEach { (_, v, c) ->
                if (v is Double) {
                    val balance = when (c) {
                        EQ, LE, GT -> 1
                        LT, GE -> -1
                        else -> 0
                    }

                    yield(Seed.Known(IEEE754Value.fromValue(v)) { known ->
                        JsPrimitiveModel(known.toDouble() + balance)
                    })
                }
            }
            DefaultFloatBound.values().forEach { bound ->
                // All numbers in JavaScript are like Double in Java/Kotlin
                yield(Seed.Known(bound(52, 11)) { known ->
                    JsPrimitiveModel(known.toDouble())
                })
            }
        }
}
