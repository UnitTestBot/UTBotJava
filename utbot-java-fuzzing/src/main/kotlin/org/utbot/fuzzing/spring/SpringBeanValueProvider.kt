package org.utbot.fuzzing.spring

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.providers.SPRING_BEAN_PROP

class SpringBeanValueProvider(
    private val idGenerator: IdGenerator<Int>,
    private val beanNameProvider: (ClassId) -> List<String>,
    private val relevantRepositories: List<SpringRepositoryId>
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
                        createBeanModel(
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
                                val entityValue = values[0]
                                val model = self.model as UtAssembleModel
                                val modificationChain: MutableList<UtStatementModel> =
                                    model.modificationsChain as MutableList<UtStatementModel>
                                modificationChain.add(
                                    UtExecutableCallModel(
                                        instance = createBeanModel(
                                            beanName = repositoryId.repositoryBeanName,
                                            id = idGenerator.createId(),
                                            classId = repositoryId.repositoryClassId,
                                        ),
                                        executable = createSaveMethodId,
                                        params = listOf(entityValue.model)
                                    )
                                )
                            })
                        }
                    },
                    empty = Routine.Empty {
                        UtNullModel(type.classId).fuzzed {
                            summary = "%var% = null"
                        }
                    }
                )
            )
        }
    }

    companion object {
        private val getBeanMethodId = MethodId(
            classId = UtSpringContextModel().classId,
            name = "getBean",
            returnType = Any::class.id,
            parameters = listOf(String::class.id),
            bypassesSandbox = true // TODO may be we can use some alternative sandbox that has more permissions
        )

        val createSaveMethodId = MethodId(
            classId = ClassId("org.springframework.data.repository.CrudRepository"),
            name = "save",
            returnType = Any::class.id,
            parameters = listOf(Any::class.id)
        )

        fun createBeanModel(beanName: String, id: Int, classId: ClassId) = UtAssembleModel(
            id = id,
            classId = classId,
            modelName = "@Autowired $beanName",
            instantiationCall = UtExecutableCallModel(
                instance = UtSpringContextModel(),
                executable = getBeanMethodId,
                params = listOf(UtPrimitiveModel(beanName))
            ),
            modificationsChainProvider = { mutableListOf() }
        )
    }
}