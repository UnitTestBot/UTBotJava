package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsStringClassId
import fuzzer.JsFuzzedValue
import fuzzer.JsMethodDescription
import fuzzer.fuzzed


import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.StringValue

object StringValueProvider : ValueProvider<JsClassId, JsFuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic
    }

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ): Sequence<Seed<JsClassId, JsFuzzedValue>> = sequence {
        val constants = description.concreteValues.asSequence()
            .filter { it.classId == jsStringClassId }
        val values = constants
            .mapNotNull { it.value as? String } +
                sequenceOf("", "abc", "\n\t\n")
        values.forEach { value ->
            yield(Seed.Known(StringValue(value)) { known ->
                JsPrimitiveModel(known.value).fuzzed {
                    summary = "%var% = '${known.value}'"
                }
            })
        }
    }
}
