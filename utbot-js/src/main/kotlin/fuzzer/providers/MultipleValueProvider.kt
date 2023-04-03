package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsMultipleClassId
import fuzzer.JsMethodDescription
import fuzzer.defaultValueProviders
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

object MultipleValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type is JsMultipleClassId
    }

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, UtModel>> =
        sequence {
            (type as JsMultipleClassId).classIds.zip(defaultValueProviders()).forEach { (classId, provider) ->
                if (provider.accept(classId)) {
                    provider.generate(description, classId)
                }
            }
        }
}
