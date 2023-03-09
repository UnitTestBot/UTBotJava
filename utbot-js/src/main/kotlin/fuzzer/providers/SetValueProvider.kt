package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.util.isJsSet
import framework.api.js.util.jsBasic
import framework.api.js.util.jsUndefinedClassId
import fuzzer.JsIdProvider
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

object SetValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean = type.isJsSet

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ) = sequence<Seed<JsClassId, UtModel>> {
        val modifications = mutableListOf<Routine.Call<JsClassId, UtModel>>()
        jsBasic.forEach { typeParameter ->
            modifications += Routine.Call(listOf(typeParameter)) { instance, arguments ->
                val model = instance as UtAssembleModel
                (model).modificationsChain as MutableList +=
                    UtExecutableCallModel(
                        model,
                        JsMethodId(
                            classId = type,
                            name = "add",
                            returnTypeNotLazy = jsUndefinedClassId,
                            parametersNotLazy = listOf(jsUndefinedClassId)
                        ),
                        arguments
                    )
            }
        }
        yield(
            Seed.Recursive(
                construct = Routine.Create(listOf(jsUndefinedClassId)) {
                    UtAssembleModel(
                        id = JsIdProvider.createId(),
                        classId = type,
                        modelName = "",
                        instantiationCall = UtExecutableCallModel(
                            null,
                            ConstructorId(type, emptyList()),
                            emptyList()
                        ),
                        modificationsChainProvider = { mutableListOf() }
                    )
                },
                empty = Routine.Empty {
                    UtAssembleModel(
                        id = JsIdProvider.createId(),
                        classId = type,
                        modelName = "",
                        instantiationCall = UtExecutableCallModel(
                            null,
                            ConstructorId(type, emptyList()),
                            emptyList()
                        )
                    )
                },
                modify = modifications.asSequence()
            )
        )
    }
}
