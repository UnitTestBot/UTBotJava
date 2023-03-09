package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.util.defaultJsValueModel
import framework.api.js.util.isJsArray
import fuzzer.JsIdProvider
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

class ArrayValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean = type.isJsArray

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ) = sequence<Seed<JsClassId, UtModel>> {
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    UtArrayModel(
                        id = JsIdProvider.createId(),
                        classId = type,
                        length = it,
                        constModel = (type.elementClassId!! as JsClassId).defaultJsValueModel(),
                        stores = hashMapOf(),
                    )
                },
                modify = Routine.ForEach(listOf(type.elementClassId!! as JsClassId)) { self, i, values ->
                    (self as UtArrayModel).stores[i] = values.first()
                }
            ))
    }
}
