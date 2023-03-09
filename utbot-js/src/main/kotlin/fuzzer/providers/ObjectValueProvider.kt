package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsConstructorId
import framework.api.js.util.isClass
import fuzzer.JsIdProvider
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel


import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.utils.hex

class ObjectValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isClass
    }

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ) = sequence {
        val constructor = type.constructor ?: JsConstructorId(type, emptyList())
        yield(createValue(type, constructor))
    }

    private fun createValue(
        classId: JsClassId,
        constructorId: JsConstructorId
    ): Seed.Recursive<JsClassId, UtModel> {
        return Seed.Recursive(
            construct = Routine.Create(constructorId.parameters) { values ->
                val id = JsIdProvider.createId()
                UtAssembleModel(
                    id = id,
                    classId = classId,
                    modelName = "${constructorId.classId.name}${constructorId.parameters}#" + id.hex(),
                    instantiationCall = UtExecutableCallModel(
                        null,
                        constructorId,
                        values
                    ),
                    modificationsChainProvider = { mutableListOf() }
                )
            },
            modify = emptySequence(),
            empty = Routine.Empty {
                UtNullModel(classId)
            }
        )
    }
}
