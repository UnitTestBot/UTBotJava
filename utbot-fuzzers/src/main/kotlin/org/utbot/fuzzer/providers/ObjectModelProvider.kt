package org.utbot.fuzzer.providers

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
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier.*

/**
 * Creates [UtAssembleModel] for objects which have public constructors
 */
class ObjectModelProvider(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    recursionDepthLeft: Int = 1,
) : RecursiveModelProvider(idGenerator, recursionDepthLeft) {
    override fun createNewInstance(parentProvider: RecursiveModelProvider, newTotalLimit: Int): RecursiveModelProvider =
        ObjectModelProvider(parentProvider.idGenerator, parentProvider.recursionDepthLeft - 1)
            .copySettingsFrom(parentProvider)
            .apply {
                totalLimit = newTotalLimit
                branchingLimit = 1     // When called recursively, we will use only simplest constructor
            }

    override fun generateModelConstructors(
        description: FuzzedMethodDescription,
        classId: ClassId
    ): List<ModelConstructor> {
        if (classId == stringClassId || classId.isPrimitiveWrapper)
            return listOf()

        val constructors = collectConstructors(classId) { javaConstructor ->
            isAccessible(javaConstructor, description.packageName)
        }.sortedWith(
            primitiveParameterizedConstructorsFirstAndThenByParameterCount
        )

        return buildList {

            constructors.forEach { constructorId ->
                with(constructorId) {
                    add(
                        ModelConstructor(parameters) { assembleModel(idGenerator.createId(), constructorId, it) }
                    )
                    if (parameters.isEmpty()) {
                        val fields = findSuitableFields(this.classId, description)
                        if (fields.isNotEmpty()) {
                            add(
                                ModelConstructor(fields.map { it.classId }) {
                                    generateModelsWithFieldsInitialization(this, fields, it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generateModelsWithFieldsInitialization(
        constructorId: ConstructorId,
        fields: List<FieldDescription>,
        fieldValues: List<FuzzedValue>
    ): FuzzedValue {
        val fuzzedModel = assembleModel(idGenerator.createId(), constructorId, emptyList())
        val assembleModel = fuzzedModel.model as? UtAssembleModel
            ?: error("Expected UtAssembleModel but ${fuzzedModel.model::class.java} found")
        val modificationChain =
            assembleModel.modificationsChain as? MutableList ?: error("Modification chain must be mutable")
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
                    MethodId(
                        constructorId.classId,
                        field.setter.name,
                        field.setter.returnType.id,
                        listOf(field.classId)
                    ),
                    listOf(value.model)
                )
                else -> null
            }
        }.forEach(modificationChain::add)
        return fuzzedModel
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