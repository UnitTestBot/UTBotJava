package org.utbot.fuzzing.spring

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.providers.SPRING_BEAN_PROP
import org.utbot.fuzzing.providers.defaultValueRoutine

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
                Seed.Recursive<FuzzedType, FuzzedValue>(
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
                                listOf(toFuzzerType(repositoryId.entityClassId.jClass, description.typeCache))
                            ) { self, values ->
                                val entityValue = values.single()
                                val model = self.model as UtAssembleModel
                                val modificationChain: MutableList<UtStatementModel> =
                                    model.modificationsChain as MutableList<UtStatementModel>
                                modificationChain.add(
                                    SpringModelUtils.createSaveCallModel(
                                        repositoryId = repositoryId,
                                        id = idGenerator.createId(),
                                        entityModel = entityValue.model
                                    )
                                )
                            })
                        }
                    },
                    empty = defaultValueRoutine(type.classId)
                )
            )
        }
    }
}