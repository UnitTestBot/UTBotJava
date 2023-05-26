package org.utbot.fuzzing.type.factories

import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.toFuzzerType
import java.lang.reflect.Type

class SimpleFuzzedTypeFactory : FuzzedTypeFactory {
    // For a concrete fuzzing run we need to track types we create.
    // Because of generics can be declared as recursive structures like `<T extends Iterable<T>>`,
    // we should track them by reference and do not call `equals` and `hashCode` recursively.
    private val cache: MutableMap<Type, FuzzedType> = mutableMapOf()

    override fun createFuzzedType(type: Type, isThisInstance: Boolean): FuzzedType =
        toFuzzerType(type, cache)
}