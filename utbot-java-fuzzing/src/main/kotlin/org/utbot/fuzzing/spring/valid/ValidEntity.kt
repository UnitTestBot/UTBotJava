package org.utbot.fuzzing.spring.valid

import org.utbot.common.toDynamicProperties
import org.utbot.common.withValue
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtSpringEntityManagerModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.entityClassIds
import org.utbot.framework.plugin.api.util.SpringModelUtils.generatedValueClassIds
import org.utbot.framework.plugin.api.util.SpringModelUtils.detachMethodIdOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.emailClassIds
import org.utbot.framework.plugin.api.util.SpringModelUtils.persistMethodIdOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.idClassIds
import org.utbot.framework.plugin.api.util.SpringModelUtils.notBlankClassIds
import org.utbot.framework.plugin.api.util.SpringModelUtils.notEmptyClassIds
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.jField
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.providers.findAccessibleModifiableFields
import org.utbot.fuzzing.providers.nullRoutine
import org.utbot.fuzzing.spring.PreservableFuzzedTypeProperty
import org.utbot.fuzzing.spring.addProperties
import org.utbot.fuzzing.spring.properties
import org.utbot.fuzzing.utils.hex

enum class EntityLifecycleState(
    val fieldAnnotationsToAvoidInitializing: List<ClassId> = emptyList(),
    val entityManagerMethodsGetter: () -> List<MethodId> = { emptyList() },
) {
    NEW_WITHOUT_GENERATED_VALUES(generatedValueClassIds),
    NEW_WITHOUT_ID(idClassIds),
    NEW,
    MANAGED(generatedValueClassIds + idClassIds, { listOfNotNull(persistMethodIdOrNull) }),
    DETACHED(generatedValueClassIds + idClassIds, { listOfNotNull(persistMethodIdOrNull, detachMethodIdOrNull) }),
}

object EntityLifecycleStateProperty : PreservableFuzzedTypeProperty<EntityLifecycleState>

class ValidEntityValueProvider(
    val idGenerator: IdGenerator<Int>,
    val onlyAcceptWhenValidIsRequired: Boolean
) : JavaValueProvider {
    override fun accept(type: FuzzedType): Boolean {
        return (!onlyAcceptWhenValidIsRequired || EntityLifecycleStateProperty in type.properties) &&
                entityClassIds.any {
                    @Suppress("UNCHECKED_CAST")
                    type.classId.jClass.getAnnotation(it.jClass as Class<out Annotation>) != null
                }
    }

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        sequence {
            val lifecycleStates = type.properties[EntityLifecycleStateProperty]?.let { listOf(it) } ?:
                EntityLifecycleState.values().toList()
            lifecycleStates.forEach { lifecycleState ->
                generateForLifecycleState(description, type.classId, lifecycleState)?.let { yield(it) }
            }
        }

    private fun generateForLifecycleState(
        description: FuzzedDescription,
        classId: ClassId,
        lifecycleState: EntityLifecycleState
    ): Seed.Recursive<FuzzedType, FuzzedValue>? {
        val noArgConstructorId = try {
            classId.jClass.getDeclaredConstructor().executableId
        } catch (e: NoSuchMethodException) {
            return null
        }
        return Seed.Recursive(
            construct = Routine.Create(types = emptyList()) { _ ->
                val id = idGenerator.createId()
                UtAssembleModel(
                    id = id,
                    classId = classId,
                    modelName = "${noArgConstructorId.classId.name}${noArgConstructorId.parameters}#" + id.hex(),
                    instantiationCall = UtExecutableCallModel(null, noArgConstructorId, params = emptyList()),
                    modificationsChainProvider = {
                        lifecycleState.entityManagerMethodsGetter().map { methodId ->
                            UtExecutableCallModel(
                                instance = UtSpringEntityManagerModel(),
                                executable = methodId,
                                params = listOf(this),
                            )
                        }
                    }
                ).fuzzed {
                    summary = "%var% = ${classId.simpleName}()"
                }
            },
            modify = sequence {
                // TODO maybe all fields
                findAccessibleModifiableFields(
                    description,
                    classId,
                    description.description.packageName
                ).forEach { fd ->
                    val field = classId.allDeclaredFieldIds.first { it.name == fd.name }.jField
                    if (lifecycleState.fieldAnnotationsToAvoidInitializing.any {
                            @Suppress("UNCHECKED_CAST")
                            field.getAnnotation(it.jClass as Class<out Annotation>) != null
                        }) return@forEach

                    val validationProperties = field.annotatedType.annotations.mapNotNull { annotation ->
                        when (annotation.annotationClass.id) {
                            in notEmptyClassIds -> NotEmptyTypeFlag.withValue(Unit)
                            in notBlankClassIds -> NotBlankTypeFlag.withValue(Unit)
                            in emailClassIds -> EmailTypeFlag.withValue(Unit)
                            // TODO support more validators
                            else -> null
                        }
                    }.toDynamicProperties()

                    val typeWithProperties = fd.type.addProperties(validationProperties)

                    when {
                        fd.canBeSetDirectly -> {
                            yield(Routine.Call(listOf(typeWithProperties)) { self, values ->
                                val model = self.model as UtAssembleModel
                                (model.modificationsChain as MutableList).add(
                                    index = 0, // prepending extra modifications to keep `persist()` modification last
                                    UtDirectSetFieldModel(
                                        model,
                                        FieldId(classId, fd.name),
                                        values.first().model
                                    )
                                )
                            })
                        }

                        fd.setter != null -> {
                            yield(Routine.Call(listOf(typeWithProperties)) { self, values ->
                                val model = self.model as UtAssembleModel
                                (model.modificationsChain as MutableList).add(
                                    index = 0, // prepending extra modifications to keep `persist()` modification last
                                    UtExecutableCallModel(
                                        model,
                                        fd.setter.executableId,
                                        values.map { it.model }
                                    )
                                )
                            })
                        }
                    }
                }
            },
            empty = nullRoutine(classId)
        )
    }
}
