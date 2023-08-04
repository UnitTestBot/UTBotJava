package org.utbot.fuzzing.spring

import org.utbot.common.dynamicPropertiesOf
import org.utbot.common.withValue
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.SpringModelUtils.persistMethodIdOrNull
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.providers.SPRING_BEAN_PROP
import org.utbot.fuzzing.providers.nullRoutine
import org.utbot.fuzzing.spring.valid.EntityLifecycleState
import org.utbot.fuzzing.spring.valid.EntityLifecycleStateProperty

class SpringBeanValueProvider(
    private val idGenerator: IdGenerator<Int>,
    private val beanNameProvider: (ClassId) -> List<String>,
    private val relevantRepositories: Set<SpringRepositoryId>
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {

    override fun enrich(description: FuzzedDescription, type: FuzzedType, scope: Scope) {
        if (description.description.isStatic == false
            && scope.parameterIndex == 0
            && scope.recursionDepth == 1) {
            scope.putProperty(SPRING_BEAN_PROP, beanNameProvider)
        }
    }

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        val beans = description.scope?.getProperty(SPRING_BEAN_PROP)
        beans?.invoke(type.classId)?.forEach { beanName ->
            yield(
                Seed.Recursive(
                    construct = Routine.Create(types = emptyList()) {
                        SpringModelUtils.createBeanModel(
                            beanName = beanName,
                            id = idGenerator.createId(),
                            classId = type.classId,
                        ).fuzzed { summary = "@Autowired ${type.classId.simpleName} $beanName" }
                    },
                    modify = sequence {
                        // TODO mutate model itself (not just repositories)
                        relevantRepositories.forEach { repositoryId ->
                            yield(Routine.Call(
                                listOf(toFuzzerType(repositoryId.entityClassId.jClass, description.typeCache).addProperties(
                                    dynamicPropertiesOf(
                                        EntityLifecycleStateProperty.withValue(EntityLifecycleState.MANAGED)
                                    )
                                ))
                            ) { selfValue, (entityValue) ->
                                val self = selfValue.model as UtAssembleModel
                                val modificationChain: MutableList<UtStatementModel> =
                                    self.modificationsChain as MutableList<UtStatementModel>
                                val entity = entityValue.model
                                if (entity is UtReferenceModel) {
                                    persistMethodIdOrNull?.let { persistMethodId ->
                                        ((entity as? UtAssembleModel)?.modificationsChain as? MutableList)?.removeAll {
                                            it is UtExecutableCallModel && it.executable == persistMethodId
                                        }
                                        modificationChain.add(
                                            UtExecutableCallModel(
                                                instance = UtSpringEntityManagerModel(),
                                                executable = persistMethodId,
                                                params = listOf(entity)
                                            )
                                        )
                                    }

                                }
                            })
                        }
                    },
                    empty = nullRoutine(type.classId)
                )
            )
        }
    }
}