package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.util.isJsMap
import framework.api.js.util.jsBasic
import framework.api.js.util.jsUndefinedClassId
import fuzzer.JsIdProvider
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.PrimitivesModelProvider.fuzzed
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

object MapValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean = type.isJsMap

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ) = sequence<Seed<JsClassId, FuzzedValue>> {
        val modifications = mutableListOf<Routine.Call<JsClassId, FuzzedValue>>()
        jsBasic.zip(jsBasic).map { (a, b) -> listOf(a, b) }.forEach { typeParameters ->
            modifications += Routine.Call(typeParameters) { instance, arguments ->
                val model = instance.model as UtAssembleModel
                (model).modificationsChain as MutableList +=
                    UtExecutableCallModel(
                        model,
                        JsMethodId(
                            classId = type,
                            name = "set",
                            returnTypeNotLazy = jsUndefinedClassId,
                            parametersNotLazy = listOf(jsUndefinedClassId, jsUndefinedClassId)
                        ),
                        arguments.map { it.model }
                    )
            }
        }
        yield(
            Seed.Recursive(
                construct = Routine.Create(listOf(jsUndefinedClassId, jsUndefinedClassId)) {
                    UtAssembleModel(
                        id = JsIdProvider.get(),
                        classId = type,
                        modelName = "",
                        instantiationCall = UtExecutableCallModel(
                            null,
                            ConstructorId(type, emptyList()),
                            emptyList()
                        ),
                        modificationsChainProvider = { mutableListOf() }
                    ).fuzzed {
                        summary = "%var% = collection"
                    }
                },
                empty = Routine.Empty {
                    UtAssembleModel(
                        id = JsIdProvider.get(),
                        classId = type,
                        modelName = "",
                        instantiationCall = UtExecutableCallModel(
                            null,
                            ConstructorId(type, emptyList()),
                            emptyList()
                        )
                    ).fuzzed {
                        summary = "%var% = collection"
                    }
                },
                modify = modifications.asSequence()
            )
        )
    }
}
