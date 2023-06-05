package fuzzer.providers


import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.util.isJsMap
import framework.api.js.util.jsUndefinedClassId
import fuzzer.JsIdProvider
import fuzzer.JsMethodDescription
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

object MapValueProvider : ValueProvider<JsClassId, UtModel, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean = type.isJsMap

    override fun generate(
        description: JsMethodDescription,
        type: JsClassId
    ) = sequence<Seed<JsClassId, UtModel>> {
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    UtAssembleModel(
                        id = JsIdProvider.createId(),
                        classId = type,
                        modelName = "",
                        instantiationCall = UtStatementCallModel(
                            null,
                            ConstructorId(type, emptyList()),
                            emptyList()
                        ),
                        modificationsChainProvider = { mutableListOf() }
                    )
                },
                modify = Routine.ForEach(listOf(jsUndefinedClassId, jsUndefinedClassId)) { self, _, values ->
                    val model = self as UtAssembleModel
                    model.modificationsChain as MutableList +=
                        UtStatementCallModel(
                            model,
                            JsMethodId(
                                classId = type,
                                name = "set",
                                returnTypeNotLazy = jsUndefinedClassId,
                                parametersNotLazy = listOf(jsUndefinedClassId, jsUndefinedClassId)
                            ),
                            values
                        )
                }
            )
        )
    }
}
