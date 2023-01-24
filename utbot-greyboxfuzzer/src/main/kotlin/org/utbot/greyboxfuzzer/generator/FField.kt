package org.utbot.greyboxfuzzer.generator

import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.util.toClass
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.ClassId
import java.lang.reflect.Field
import java.lang.reflect.Type

data class FField(
    val field: Field?,
    val value: Any?,
    val resolvedType: Type,
    val generator: Generator?,
    val classId: ClassId,
    val subFields: List<FField>,
    var isBlocked: Boolean,
) {

    constructor(
        field: Field?,
        value: Any?,
        resolvedType: Type,
        generator: Generator?,
        subFields: List<FField>,
        isBlocked: Boolean
    ) : this(
        field,
        value,
        resolvedType,
        generator,
        classIdForType(field?.type ?: resolvedType.toClass()!!),
        subFields,
        isBlocked
    )

    constructor(
        field: Field?,
        value: Any?,
        resolvedType: Type,
        generator: Generator?,
        subFields: List<FField>,
    ) : this(
        field,
        value,
        resolvedType,
        generator,
        classIdForType(field?.type ?: resolvedType.toClass()!!),
        subFields,
        false
    )

    constructor(
        field: Field?,
        value: Any?,
        resolvedType: Type,
        generator: Generator?,
    ) : this(
        field,
        value,
        resolvedType,
        generator,
        classIdForType(field?.type ?: resolvedType.toClass()!!),
        listOf(),
        false
    )

    constructor(
        field: Field?,
        value: Any?,
        resolvedType: Type
    ) : this(field, value, resolvedType, null, classIdForType(field?.type ?: resolvedType.toClass()!!), listOf(), false)

}