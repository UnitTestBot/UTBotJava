package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import fuzzer.JsFuzzedValue
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.Bool

object BoolValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic
    }

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, UtModel>> =
        sequence {
            yield(Seed.Known(Bool.TRUE()) {
                JsPrimitiveModel(true)
            })
            yield(Seed.Known(Bool.FALSE()) {
                JsPrimitiveModel(false)
            })
        }
}
