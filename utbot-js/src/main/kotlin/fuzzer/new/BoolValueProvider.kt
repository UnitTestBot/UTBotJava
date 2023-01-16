package fuzzer.new

import framework.api.js.JsClassId
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsBooleanClassId
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Bool

object BoolValueProvider: ValueProvider<JsClassId, JsFuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic || type == jsBooleanClassId
    }

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, JsFuzzedValue>> = sequence {
        yield(Seed.Known(Bool.TRUE()) { JsFuzzedValue(true) })
        yield(Seed.Known(Bool.FALSE()))
    }
}