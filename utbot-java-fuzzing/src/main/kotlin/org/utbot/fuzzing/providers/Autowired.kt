package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.*
import org.utbot.fuzzing.*

class AutowiredValueProvider(
    private val idGenerator: IdGenerator<Int>,
    private val autowiredModelOriginCreator: (beanName: String) -> UtModel
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    override fun accept(type: FuzzedType) = type is AutowiredFuzzedType && type.beanNames.isNotEmpty()

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        (type as AutowiredFuzzedType).beanNames.forEach { beanName ->
            yield(
                Seed.Recursive<FuzzedType, FuzzedValue>(
                    construct = Routine.Create(types = emptyList()) {
                        UtAutowiredStateBeforeModel(
                            id = idGenerator.createId(),
                            classId = type.classId,
                            beanName = beanName,
                            origin = autowiredModelOriginCreator(beanName), // TODO think about setting origin id and its fields ids
                            // TODO properly detect which repositories need to be filled up (right now orderRepository is hardcoded)
                            repositoriesContent = listOf(
                                RepositoryContentModel(
                                    repositoryBeanName = "orderRepository",
                                    entityModels = mutableListOf()
                                )
                            ),
                        ).fuzzed { "@Autowired ${type.classId.simpleName} $beanName" }
                    },
                    modify = sequence {
                        // TODO mutate model itself (not just repositories)
                        // TODO properly detect which repositories need to be filled up (right now orderRepository is hardcoded)
                        yield(Routine.Call(
                            listOf(FuzzedType(ClassId("com.rest.order.models.Order"))),
                        ) { self, values ->
                            val entityValue = values[0]
                            val model = self.model as UtAutowiredStateBeforeModel
                            // TODO maybe use `entityValue.summary` to update `model.summary`
                            model.repositoriesContent
                                .first { it.repositoryBeanName == "orderRepository" }
                                .entityModels as MutableList += entityValue.model
                        })
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
}