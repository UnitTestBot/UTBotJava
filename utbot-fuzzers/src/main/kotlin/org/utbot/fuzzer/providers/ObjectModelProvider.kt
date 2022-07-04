package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.exceptIsInstance
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.objectModelProviders
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.util.function.BiConsumer
import java.util.function.IntSupplier

/**
 * Creates [UtAssembleModel] for objects which have public constructors with primitives types and String as parameters.
 */
class ObjectModelProvider : ModelProvider {

    var modelProvider: ModelProvider

    private val idGenerator: IntSupplier
    private val recursion: Int
    private val limit: Int

    constructor(idGenerator: IntSupplier) : this(idGenerator, Int.MAX_VALUE)

    constructor(idGenerator: IntSupplier, limit: Int) : this(idGenerator, limit, 1)

    private constructor(idGenerator: IntSupplier, limit: Int, recursion: Int) {
        this.idGenerator = idGenerator
        this.recursion = recursion
        this.limit = limit
        this.modelProvider = objectModelProviders(idGenerator)
    }

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
        val fuzzedValues = with(description) {
            parameters.asSequence()
                .filterNot { it == stringClassId || it.isPrimitiveWrapper }
                .flatMap { classId ->
                    collectConstructors(classId) { javaConstructor ->
                        isPublic(javaConstructor)
                    }.sortedWith(
                        primitiveParameterizedConstructorsFirstAndThenByParameterCount
                    ).take(limit)
                }
                    .associateWith { constructorId ->
                    val modelProviderWithoutRecursion = modelProvider.exceptIsInstance<ObjectModelProvider>()
                    fuzzParameters(
                        constructorId,
                        if (recursion > 0) {
                            ObjectModelProvider(idGenerator, limit = 1, recursion - 1).with(modelProviderWithoutRecursion)
                        } else {
                            modelProviderWithoutRecursion.withFallback(NullModelProvider)
                        }
                    )
                }
                .flatMap { (constructorId, fuzzedParameters) ->
                    if (constructorId.parameters.isEmpty()) {
                        sequenceOf(assembleModel(idGenerator.asInt, constructorId, emptyList()))
                    }
                    else {
                        fuzzedParameters.map { params ->
                            assembleModel(idGenerator.asInt, constructorId, params)
                        }
                    }
                }
        }

        fuzzedValues.forEach { fuzzedValue ->
            description.parametersMap[fuzzedValue.model.classId]?.forEach { index ->
                consumer.accept(index, fuzzedValue)
            }
        }
    }

    companion object {
        private fun collectConstructors(classId: ClassId, predicate: (Constructor<*>) -> Boolean): Sequence<ConstructorId> {
            return classId.jClass.declaredConstructors.asSequence()
                .filter(predicate)
                .map { javaConstructor ->
                    ConstructorId(classId, javaConstructor.parameters.map { it.type.id })
                }
        }

        private fun isPublic(javaConstructor: Constructor<*>): Boolean {
            return javaConstructor.modifiers and Modifier.PUBLIC != 0
        }

        private fun FuzzedMethodDescription.fuzzParameters(constructorId: ConstructorId, vararg modelProviders: ModelProvider): Sequence<List<FuzzedValue>> {
            val fuzzedMethod = FuzzedMethodDescription(
                executableId = constructorId,
                concreteValues = this.concreteValues
            )
            return fuzz(fuzzedMethod, *modelProviders)
        }

        private fun assembleModel(id: Int, constructorId: ConstructorId, params: List<FuzzedValue>): FuzzedValue {
            val instantiationChain = mutableListOf<UtStatementModel>()
            return UtAssembleModel(
                id,
                constructorId.classId,
                "${constructorId.classId.name}${constructorId.parameters}#" + id.toString(16),
                instantiationChain
            ).apply {
                instantiationChain += UtExecutableCallModel(null, constructorId, params.map { it.model }, this)
            }.fuzzed {
                summary = "%var% = ${constructorId.classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
            }
        }

        private val primitiveParameterizedConstructorsFirstAndThenByParameterCount =
            compareByDescending<ConstructorId> { constructorId ->
                constructorId.parameters.all { classId ->
                    classId.isPrimitive || classId == stringClassId
                }
            }.thenComparingInt { constructorId ->
                constructorId.parameters.size
            }
    }
}