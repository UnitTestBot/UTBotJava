package fuzzer.new

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsStringClassId
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.PrimitivesModelProvider.fuzzed
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.StringValue

object StringValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic || type == jsStringClassId
    }

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ): Sequence<Seed<JsClassId, FuzzedValue>> = sequence {
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
