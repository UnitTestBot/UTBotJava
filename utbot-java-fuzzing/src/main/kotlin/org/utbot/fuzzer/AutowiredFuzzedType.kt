package org.utbot.fuzzer

import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.providers.CustomJavaValueProviderHolder

class AutowiredFuzzedType(
    fuzzedType: FuzzedType,
    val beanNames: List<String>,
    override val javaValueProvider: JavaValueProvider
) : FuzzedType(fuzzedType.classId, fuzzedType.generics), CustomJavaValueProviderHolder {
    override fun toString(): String {
        return "AutowiredFuzzedType(classId=$classId, generics=${generics.map { it.classId }}, beanNames=$beanNames)"
    }
}