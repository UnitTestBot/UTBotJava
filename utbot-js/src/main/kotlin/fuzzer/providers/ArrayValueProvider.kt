package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.util.defaultJsValueModel
import framework.api.js.util.isJsArray
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ReferencePreservingIntIdGenerator
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

private val idGenerator = ReferencePreservingIntIdGenerator()

class ArrayValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean = type.isJsArray

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ) = sequence<Seed<JsClassId, FuzzedValue>> {
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    UtArrayModel(
                        id = idGenerator.createId(),
                        classId = type,
                        length = it,
                        constModel = (type.elementClassId!! as JsClassId).defaultJsValueModel(),
                        stores = hashMapOf(),
                    ).fuzzed {
                        summary = "%var% = ${type.elementClassId!!.simpleName}[$it]"
                    }
                },
                modify = Routine.ForEach(listOf(type.elementClassId!! as JsClassId)) { self, i, values ->
                    (self.model as UtArrayModel).stores[i] = values.first().model
                }
            ))
    }
}