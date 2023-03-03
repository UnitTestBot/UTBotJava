package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import fuzzer.JsMethodDescription


import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.Bool

object BoolValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic
    }

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, FuzzedValue>> =
        sequence {
            yield(Seed.Known(Bool.TRUE()) {
                JsPrimitiveModel(true).fuzzed {
                    summary = "%var% = true"
                }
            })
            yield(Seed.Known(Bool.FALSE()) {
                JsPrimitiveModel(false).fuzzed {
                    summary = "%var% = false"
                }
            })
        }
}
