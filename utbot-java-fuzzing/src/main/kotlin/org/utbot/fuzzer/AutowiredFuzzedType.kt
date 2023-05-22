package org.utbot.fuzzer

class AutowiredFuzzedType(
    fuzzedType: FuzzedType,
    val beanNames: List<String>
) : FuzzedType(fuzzedType.classId, fuzzedType.generics) {
    override val usesCustomValueProvider get() = beanNames.isNotEmpty()

    override fun toString(): String {
        return "AutowiredFuzzedType(classId=$classId, generics=${generics.map { it.classId }}, beanNames=$beanNames)"
    }
}