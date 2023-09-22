package org.utbot.fuzzing.spring

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.toTypeParametrizedByTypeVariables
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.providers.nullRoutine
import org.utbot.fuzzing.toFuzzerType

class JavaLangObject(
    private val classesToTryUsingAsJavaLangObject: List<ClassId>,
) : JavaValueProvider {
    override fun accept(type: FuzzedType): Boolean {
        return type.classId == objectClassId
    }

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        classesToTryUsingAsJavaLangObject.map { classToUseAsObject ->
            val fuzzedType = toFuzzerType(
                type = classToUseAsObject.jClass.toTypeParametrizedByTypeVariables(),
                cache = description.typeCache
            )
            Seed.Recursive(
                construct = Routine.Create(listOf(fuzzedType)) { (value) -> value },
                modify = emptySequence(),
                empty = nullRoutine(type.classId)
            )
        }.asSequence()
}