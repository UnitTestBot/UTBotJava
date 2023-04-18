package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.util.isEnum
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

class EnumValueProvider(
    val idGenerator: IdentityPreservingIdGenerator<Int>,
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    override fun accept(type: FuzzedType) = type.classId.isEnum

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence<Seed<FuzzedType, FuzzedValue>> {
        val jClass = type.classId.jClass
        if (isAccessible(jClass, description.description.packageName)) {
            jClass.enumConstants.filterIsInstance<Enum<*>>().forEach { enum ->
                val id = idGenerator.getOrCreateIdForValue(enum)
                yield(Seed.Simple(UtEnumConstantModel(id, type.classId, enum).fuzzed {
                    summary = "%var% = $enum"
                }))
            }
        }
    }
}