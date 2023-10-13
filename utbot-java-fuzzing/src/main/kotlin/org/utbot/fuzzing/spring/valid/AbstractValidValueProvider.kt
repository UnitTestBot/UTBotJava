package org.utbot.fuzzing.spring.valid

import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.spring.FuzzedTypeProperty
import org.utbot.fuzzing.spring.properties
import org.utbot.fuzzing.spring.withoutProperty

abstract class AbstractValidValueProvider<TProp : FuzzedTypeProperty<T>, T> : JavaValueProvider {
    protected abstract val validationDataTypeProperty: TProp

    protected abstract fun acceptsType(type: FuzzedType): Boolean
    protected abstract fun makeValid(originalValue: FuzzedValue, validationData: T): FuzzedValue
    protected abstract fun defaultValidValue(validationData: T): FuzzedValue

    final override fun accept(type: FuzzedType): Boolean =
        validationDataTypeProperty in type.properties && acceptsType(type)

    final override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed.Recursive<FuzzedType, FuzzedValue>> {
        val validationData = type.properties.getValue(validationDataTypeProperty)
        return sequenceOf(
            Seed.Recursive(
                construct = Routine.Create(listOf(type.withoutProperty(validationDataTypeProperty))) { (origin) ->
                    makeValid(origin, validationData)
                },
                empty = Routine.Empty { defaultValidValue(validationData) }
            )
        )
    }
}