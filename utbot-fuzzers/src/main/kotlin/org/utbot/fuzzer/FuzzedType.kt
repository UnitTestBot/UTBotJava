package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId

/**
 * Contains information about classId and generic types, that should be applied.
 *
 * Currently, there's some limitation for generics that are supported:
 * 1. Only concrete types and collections are supported
 * 2. No relative types like: `Map<T, V extends T>`
 *
 * Note, that this class can be replaced by API mechanism for collecting parametrized types,
 * but at the moment it doesn't fully support all necessary operations.
 *
 * @see ClassId.typeParameters
 */
class FuzzedType(
    val classId: ClassId,
    val generics: List<FuzzedType> = emptyList(),
)