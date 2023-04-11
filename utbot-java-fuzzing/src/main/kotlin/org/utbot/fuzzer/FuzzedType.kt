package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId

/**
 * Contains information about classId and generic types, that should be applied.
 *
 * Note that this class can be replaced by the API mechanism for collecting parametrized types,
 * but at the moment it doesn't fully support all necessary operations.
 *
 * @see ClassId.typeParameters
 */
class FuzzedType(
    val classId: ClassId,
    val generics: List<FuzzedType> = emptyList(),
) {
    override fun toString(): String {
        return "FuzzedType(classId=$classId, generics=${generics.map { it.classId }})"
    }
}