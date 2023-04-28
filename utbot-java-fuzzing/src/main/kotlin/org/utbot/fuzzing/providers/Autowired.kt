package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.*
import org.utbot.fuzzing.*

class AutowiredValueProvider(
    val idGenerator: IdGenerator<Int>
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    override fun accept(type: FuzzedType) = type is AutowiredFuzzedType

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        (type as AutowiredFuzzedType).beanNames.forEach { beanName ->
            yield(
                Seed.Simple<FuzzedType, FuzzedValue>(
                    value = FuzzedValue(UtAutowiredModel(idGenerator.createId(), type.classId, beanName), "@Autowired ${type.classId.simpleName} $beanName"),
                    mutation = { fuzzedValue, _ -> fuzzedValue } // TODO implement bean mutation
                )
            )
        }
    }
}