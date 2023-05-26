package org.utbot.fuzzing.providers

import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Seed

interface CustomJavaValueProviderHolder {
    val javaValueProvider: JavaValueProvider
}

object DelegatingToCustomJavaValueProvider : JavaValueProvider {
    override fun accept(type: FuzzedType): Boolean = type is CustomJavaValueProviderHolder

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        (type as CustomJavaValueProviderHolder).javaValueProvider.generate(description, type)
}