package org.utbot.fuzzer.providers

import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.TooManyCombinationsException
import org.utbot.fuzzer.exceptIsInstance
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.objectModelProviders
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier.*

private val logger by lazy { KotlinLogging.logger {} }

/**
 * Creates [UtAssembleModel] for objects which have public constructors with primitives types and String as parameters.
 */
class ObjectModelProvider(
    private val idGenerator: IdentityPreservingIdGenerator<Int>,
    private val limit: Int = Int.MAX_VALUE,
    private val recursion: Int = 1
) : ModelProvider {

    var modelProvider: ModelProvider = objectModelProviders(idGenerator)
    var limitValuesCreatedByFieldAccessors: Int = 100
        set(value) {
            field = maxOf(0, value)
        }

    private val nonRecursiveModelProvider: ModelProvider
        get() {
            val modelProviderWithoutRecursion = modelProvider.exceptIsInstance<ObjectModelProvider>()
            return if (recursion > 0) {
                ObjectModelProvider(idGenerator, limit = 1, recursion - 1).with(modelProviderWithoutRecursion)
            } else {
                modelProviderWithoutRecursion.withFallback(NullModelProvider)
            }
        }

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val fuzzedValues = with(description) {
            parameters.asSequence()
                .filterNot { it == stringClassId || it.isPrimitiveWrapper }
                .flatMap { classId ->
                    collectConstructors(classId) { javaConstructor ->
                        isAccessible(javaConstructor, description.packageName)
                    }.sortedWith(
                        primitiveParameterizedConstructorsFirstAndThenByParameterCount
                    ).take(limit)
                }
                .associateWith { constructorId ->
                    fuzzParameters(
                        constructorId,
                        nonRecursiveModelProvider
                    )
                }
                .flatMap { (constructorId, fuzzedParameters) ->
                    if (constructorId.parameters.isEmpty()) {
                        sequenceOf(assembleModel(idGenerator.createId(), constructorId, emptyList())) +
                                generateModelsWithFieldsInitialization(constructorId, description, concreteValues)
                    }
                    else {
                        fuzzedParameters.map { params ->
                            assembleModel(idGenerator.createId(), constructorId, params)
                        }
                    }
                }
        }

        fuzzedValues.forEach { fuzzedValue ->
            description.parametersMap[fuzzedValue.model.classId]?.forEach { index ->
                yieldValue(index, fuzzedValue)
            }
        }
    }

    private fun generateModelsWithFieldsInitialization(constructorId: ConstructorId, description: FuzzedMethodDescription, concreteValues: Collection<FuzzedConcreteValue>): Sequence<FuzzedValue> {
        if (limitValuesCreatedByFieldAccessors == 0) return emptySequence()
        val fields = findSuitableFields(constructorId.classId, description)
        val syntheticClassFieldsSetterMethodDescription = FuzzedMethodDescription(
            "${constructorId.classId.simpleName}<syntheticClassFieldSetter>",
            voidClassId,
            fields.map { it.classId },
            concreteValues
        ).apply {
            packageName = description.packageName
        }

        return fuzz(syntheticClassFieldsSetterMethodDescription, nonRecursiveModelProvider)
            .take(limitValuesCreatedByFieldAccessors) // limit the number of fuzzed values in this particular case
            .map { fieldValues ->
                val fuzzedModel = assembleModel(idGenerator.createId(), constructorId, emptyList())
                val assembleModel = fuzzedModel.model as? UtAssembleModel ?: error("Expected UtAssembleModel but ${fuzzedModel.model::class.java} found")
                val modificationChain = assembleModel.modificationsChain as? MutableList ?: error("Modification chain must be mutable")
                fieldValues.asSequence().mapIndexedNotNull { index, value ->
                    val field = fields[index]
                    when {
                        field.canBeSetDirectly -> UtDirectSetFieldModel(
                            fuzzedModel.model,
                            FieldId(constructorId.classId, field.name),
                            value.model
                        )
                        field.setter != null -> UtExecutableCallModel(
                            fuzzedModel.model,
                            MethodId(constructorId.classId, field.setter.name, field.setter.returnType.id, listOf(field.classId)),
                            listOf(value.model)
                        )
                        else -> null
                    }
                }.forEach(modificationChain::add)
                fuzzedModel
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

        private fun isAccessible(member: Member, packageName: String?): Boolean {
            return isPublic(member.modifiers) ||
                    (packageName != null && isPackagePrivate(member.modifiers) && member.declaringClass.`package`?.name == packageName)
        }

        private fun isPackagePrivate(modifiers: Int): Boolean {
            val hasAnyAccessModifier = isPrivate(modifiers)
                    || isProtected(modifiers)
                    || isProtected(modifiers)
            return !hasAnyAccessModifier
        }

        private fun FuzzedMethodDescription.fuzzParameters(constructorId: ConstructorId, vararg modelProviders: ModelProvider): Sequence<List<FuzzedValue>> {
            val fuzzedMethod = FuzzedMethodDescription(
                executableId = constructorId,
                concreteValues = this.concreteValues
            ).apply {
                this.packageName = this@fuzzParameters.packageName
            }
            return try {
                fuzz(fuzzedMethod, *modelProviders)
            } catch (t: TooManyCombinationsException) {
                logger.warn(t) { "Number of combination of ${parameters.size} parameters is huge. Fuzzing is skipped for $name" }
                emptySequence()
            }
        }

        private fun assembleModel(id: Int, constructorId: ConstructorId, params: List<FuzzedValue>): FuzzedValue {
            val instantiationChain = mutableListOf<UtStatementModel>()
            return UtAssembleModel(
                id,
                constructorId.classId,
                "${constructorId.classId.name}${constructorId.parameters}#" + id.toString(16),
                instantiationChain = instantiationChain,
                modificationsChain = mutableListOf()
            ).apply {
                instantiationChain += UtExecutableCallModel(null, constructorId, params.map { it.model }, this)
            }.fuzzed {
                summary = "%var% = ${constructorId.classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
            }
        }

        private fun findSuitableFields(classId: ClassId, description: FuzzedMethodDescription): List<FieldDescription>  {
            val jClass = classId.jClass
            return jClass.declaredFields.map { field ->
                FieldDescription(
                    field.name,
                    field.type.id,
                    isAccessible(field, description.packageName) && !isFinal(field.modifiers) && !isStatic(field.modifiers),
                    jClass.findPublicSetterIfHasPublicGetter(field, description)
                )
            }
        }

        private fun Class<*>.findPublicSetterIfHasPublicGetter(field: Field, description: FuzzedMethodDescription): Method? {
            val postfixName = field.name.capitalize()
            val setterName = "set$postfixName"
            val getterName = "get$postfixName"
            val getter = try { getDeclaredMethod(getterName) } catch (_: NoSuchMethodException) { return null }
            return if (isAccessible(getter, description.packageName) && getter.returnType == field.type) {
                declaredMethods.find {
                    isAccessible(it, description.packageName) &&
                            it.name == setterName &&
                            it.parameterCount == 1 &&
                            it.parameterTypes[0] == field.type
                }
            } else {
                null
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

        private class FieldDescription(
            val name: String,
            val classId: ClassId,
            val canBeSetDirectly: Boolean,
            val setter: Method?,
        )
    }
}
