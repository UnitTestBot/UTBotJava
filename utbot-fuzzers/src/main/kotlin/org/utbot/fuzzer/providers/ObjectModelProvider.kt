package org.utbot.fuzzer.providers

import java.lang.reflect.Constructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.util.isAbstract
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.dateClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isEnum
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.objects.FuzzerMockableMethodId
import org.utbot.fuzzer.objects.assembleModel
import org.utbot.fuzzing.providers.FieldDescription
import org.utbot.fuzzing.providers.findAccessibleModifiableFields
import org.utbot.fuzzing.providers.isAccessible

/**
 * Creates [UtAssembleModel] for objects which have public constructors
 */
class ObjectModelProvider(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    recursionDepthLeft: Int = 2,
) : RecursiveModelProvider(idGenerator, recursionDepthLeft) {
    override fun newInstance(parentProvider: RecursiveModelProvider, constructor: ModelConstructor): RecursiveModelProvider {
        val newInstance = ObjectModelProvider(parentProvider.idGenerator, parentProvider.recursionDepthLeft - 1)
        newInstance.copySettings(parentProvider)
        newInstance.branchingLimit = 1
        return newInstance
    }

    override fun generateModelConstructors(
        description: FuzzedMethodDescription,
        parameterIndex: Int,
        classId: ClassId,
    ): Sequence<ModelConstructor> = sequence {
        if (unwantedConstructorsClasses.contains(classId)
            || classId.isPrimitiveWrapper
            || classId.isEnum
            || classId.isAbstract
            || (classId.isInner && !classId.isStatic)
        ) return@sequence

        val constructors = collectConstructors(classId) { javaConstructor ->
            isAccessible(javaConstructor, description.packageName)
        }.sortedWith(
            primitiveParameterizedConstructorsFirstAndThenByParameterCount
        )

        constructors.forEach { constructorId ->
            // When branching limit = 1 this block tries to create new values
            // and mutate some fields. Only if there's no option next block
            // with empty constructor should be used.
            if (constructorId.parameters.isEmpty()) {
                val fields = findAccessibleModifiableFields(null, constructorId.classId, description.packageName)
                if (fields.isNotEmpty()) {
                    yield(
                        ModelConstructor(fields.map { it.type }) {
                            generateModelsWithFieldsInitialization(constructorId, fields, it)
                        }
                    )
                }
            }
            yield(ModelConstructor(constructorId.parameters.map { classId -> FuzzedType(classId) }) {
                assembleModel(idGenerator.createId(), constructorId, it)
            })
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
                    FuzzerMockableMethodId(
                        constructorId.classId,
                        field.setter.name,
                        field.setter.returnType.id,
                        listOf(field.type.classId),
                        mock = {
                            field.getter?.let { g ->
                                val getterMethodID = MethodId(
                                    classId = constructorId.classId,
                                    name = g.name,
                                    returnType = g.returnType.id,
                                    parameters = emptyList()
                                )
                                mapOf(getterMethodID to listOf(value.model))
                            } ?: emptyMap()
                        }
                    ),
                    listOf(value.model)
                )
                else -> null
            }
        }.forEach(modificationChain::add)
        return fuzzedModel
    }

    companion object {

        private val unwantedConstructorsClasses = listOf(
            stringClassId, dateClassId
        )

        private fun collectConstructors(classId: ClassId, predicate: (Constructor<*>) -> Boolean): Sequence<ConstructorId> {
            return classId.jClass.declaredConstructors.asSequence()
                .filter(predicate)
                .map { javaConstructor ->
                    ConstructorId(classId, javaConstructor.parameters.map { it.type.id })
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