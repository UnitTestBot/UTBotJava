package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.util.function.BiConsumer
import java.util.function.ToIntFunction

/**
 * Creates [UtAssembleModel] for objects which have public constructors with primitives types and String as parameters.
 */
class ObjectModelProvider(
    private val idGenerator: ToIntFunction<ClassId>
) : ModelProvider {

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        val assembleModels = with(description) {
            parameters.asSequence()
                .flatMap { classId ->
                    collectConstructors(classId) { javaConstructor ->
                        isPublic(javaConstructor) && javaConstructor.parameters.all(::isPrimitiveOrString)
                    }
                }
                .associateWith {
                    fuzzParameters(it)
                }
                .flatMap { (constructorId, fuzzedParameters) ->
                    fuzzedParameters.map { params ->
                        assembleModel(idGenerator.applyAsInt(constructorId.classId), constructorId, params)
                    }
                }
        }

        assembleModels.forEach { assembleModel ->
            description.parametersMap[assembleModel.classId]?.forEach { index ->
                consumer.accept(index, assembleModel)
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

        private fun isPrimitiveOrString(parameter: Parameter): Boolean {
            val parameterType = parameter.type
            return parameterType.isPrimitive || String::class.java == parameterType
        }

        private fun FuzzedMethodDescription.fuzzParameters(constructorId: ConstructorId): Sequence<List<UtModel>> {
            val fuzzedMethod = FuzzedMethodDescription(
                executableId = constructorId,
                concreteValues = this.concreteValues
            )
            val modelProviders = arrayOf(
                PrimitivesModelProvider,
                ConstantsModelProvider
            )
            return fuzz(fuzzedMethod, *modelProviders)
        }

        private fun assembleModel(id: Int, constructorId: ConstructorId, params: List<UtModel>): UtAssembleModel {
            val instantiationChain = mutableListOf<UtStatementModel>()
            return UtAssembleModel(
                id,
                constructorId.classId,
                "${constructorId.classId.name}${constructorId.parameters}#" + id.toString(16),
                instantiationChain
            ).apply {
                instantiationChain += UtExecutableCallModel(null, constructorId, params, this)
            }
        }
    }
}