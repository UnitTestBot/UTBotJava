package fuzzer.providers

import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import framework.api.js.JsClassId
import framework.api.js.JsConstructorId
import framework.api.js.util.isJsBasic
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.TooManyCombinationsException
import org.utbot.fuzzer.fuzz
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntSupplier

object JsObjectModelProvider : ModelProvider {

    class SimpleIdGenerator : IntSupplier {
        private val id = AtomicInteger()
        override fun getAsInt() = id.incrementAndGet()
    }

    val idGenerator = SimpleIdGenerator()

    private val primitiveModelProviders = ModelProvider.of(
        JsConstantsModelProvider,
        JsUndefinedModelProvider,
        JsStringModelProvider,
        JsMultipleTypesModelProvider,
        JsPrimitivesModelProvider,
    )

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val fuzzedValues = with(description) {
            parameters.asSequence()
                .filterNot { (it as JsClassId).isJsBasic }
                .map { classId ->
                    val constructor = (classId as JsClassId).allConstructors.first() as JsConstructorId
                    constructor
                }.associateWith { constructor ->
                    fuzzParameters(constructor, primitiveModelProviders)
                }.flatMap { (constructor, fuzzedParams) ->
                    fuzzedParams.map { params ->
                        assemble(idGenerator.asInt, constructor, params)
                    }
                }
        }
        fuzzedValues.forEach { fuzzedValue ->
            description.parametersMap[fuzzedValue.model.classId]?.forEach { index ->
                yieldValue(index, fuzzedValue)
            }
        }
    }

    private fun assemble(id: Int, constructor: ConstructorId, values: List<FuzzedValue>): FuzzedValue {
        val instantiationCall = UtExecutableCallModel(null, constructor, values.map { it.model })
        val model = UtAssembleModel(
            id,
            constructor.classId,
            "${constructor.classId.name}${constructor.parameters}#" + id.toString(16),
            instantiationCall = instantiationCall,
        ) .fuzzed {
            summary =
                "%var% = ${constructor.classId.simpleName}(${constructor.parameters.joinToString { it.simpleName }})"
        }
        return model
    }

    private fun FuzzedMethodDescription.fuzzParameters(
        constructorId: ConstructorId,
        vararg modelProviders: ModelProvider
    ): Sequence<List<FuzzedValue>> {
        val fuzzedMethod = FuzzedMethodDescription(
            executableId = constructorId,
            concreteValues = this.concreteValues
        ).apply {
            this.packageName = this@fuzzParameters.packageName
        }
        return try {
            fuzz(fuzzedMethod, *modelProviders)
        } catch (t: TooManyCombinationsException) {
            emptySequence()
        }
    }


}