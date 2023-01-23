package fuzzer.new

import framework.api.js.JsClassId
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsStringClassId
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

object StringValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic || type == jsStringClassId
    }

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ): Sequence<Seed<JsClassId, FuzzedValue>> {
        TODO("Not yet implemented")
    }
}
