package org.utbot.framework.codegen.model.constructor.builtin

import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.model.constructor.util.utilMethodId
import org.utbot.framework.codegen.model.tree.CgClassId
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import sun.misc.Unsafe

/**
 * Set of ids of all possible util methods for a given class
 * The class may actually not have some of these methods if they
 * are not required in the process of code generation
 */
internal val ClassId.possibleUtilMethodIds: Set<MethodId>
    get() = setOf(
        getUnsafeInstanceMethodId,
        createInstanceMethodId,
        createArrayMethodId,
        setFieldMethodId,
        setStaticFieldMethodId,
        getFieldValueMethodId,
        getStaticFieldValueMethodId,
        getEnumConstantByNameMethodId,
        deepEqualsMethodId,
        arraysDeepEqualsMethodId,
        iterablesDeepEqualsMethodId,
        streamsDeepEqualsMethodId,
        mapsDeepEqualsMethodId,
        hasCustomEqualsMethodId,
        getArrayLengthMethodId
    )

internal val ClassId.getUnsafeInstanceMethodId: MethodId
    get() = utilMethodId(
            name = "getUnsafeInstance",
            returnType = Unsafe::class.id,
    )

/**
 * Method that creates instance using Unsafe
 */
internal val ClassId.createInstanceMethodId: MethodId
    get() = utilMethodId(
            name = "createInstance",
            returnType = CgClassId(objectClassId, isNullable = true),
            arguments = arrayOf(stringClassId)
    )

internal val ClassId.createArrayMethodId: MethodId
    get() = utilMethodId(
            name = "createArray",
            returnType = Array<Any>::class.id,
            arguments = arrayOf(stringClassId, intClassId, Array<Any>::class.id)
    )

internal val ClassId.setFieldMethodId: MethodId
    get() = utilMethodId(
            name = "setField",
            returnType = voidClassId,
            arguments = arrayOf(objectClassId, stringClassId, objectClassId)
    )

internal val ClassId.setStaticFieldMethodId: MethodId
    get() = utilMethodId(
            name = "setStaticField",
            returnType = voidClassId,
            arguments = arrayOf(Class::class.id, stringClassId, objectClassId)
    )

internal val ClassId.getFieldValueMethodId: MethodId
    get() = utilMethodId(
            name = "getFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(objectClassId, stringClassId)
    )

internal val ClassId.getStaticFieldValueMethodId: MethodId
    get() = utilMethodId(
            name = "getStaticFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
    )

internal val ClassId.getEnumConstantByNameMethodId: MethodId
    get() = utilMethodId(
            name = "getEnumConstantByName",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
    )

internal val ClassId.deepEqualsMethodId: MethodId
    get() = utilMethodId(
        name = "deepEquals",
        returnType = booleanClassId,
        arguments = arrayOf(objectClassId, objectClassId)
    )

internal val ClassId.arraysDeepEqualsMethodId: MethodId
    get() = utilMethodId(
        name = "arraysDeepEquals",
        returnType = booleanClassId,
        arguments = arrayOf(objectClassId, objectClassId)
    )

internal val ClassId.iterablesDeepEqualsMethodId: MethodId
    get() = utilMethodId(
        name = "iterablesDeepEquals",
        returnType = booleanClassId,
        arguments = arrayOf(java.lang.Iterable::class.id, java.lang.Iterable::class.id)
    )

internal val ClassId.streamsDeepEqualsMethodId: MethodId
    get() = utilMethodId(
        name = "streamsDeepEquals",
        returnType = booleanClassId,
        arguments = arrayOf(java.util.stream.BaseStream::class.id, java.util.stream.BaseStream::class.id)
    )

internal val ClassId.mapsDeepEqualsMethodId: MethodId
    get() = utilMethodId(
        name = "mapsDeepEquals",
        returnType = booleanClassId,
        arguments = arrayOf(java.util.Map::class.id, java.util.Map::class.id)
    )

internal val ClassId.hasCustomEqualsMethodId: MethodId
    get() = utilMethodId(
        name = "hasCustomEquals",
        returnType = booleanClassId,
        arguments = arrayOf(Class::class.id)
    )

internal val ClassId.getArrayLengthMethodId: MethodId
    get() = utilMethodId(
        name = "getArrayLength",
        returnType = intClassId,
        arguments = arrayOf(objectClassId)
    )

/**
 * [MethodId] for [AutoCloseable.close].
 */
val closeMethodId = MethodId(
    classId = AutoCloseable::class.java.id,
    name = "close",
    returnType = voidClassId,
    parameters = emptyList()
)

val mocksAutoCloseable: Set<ClassId> = setOf(
    MockitoStaticMocking.mockedStaticClassId,
    MockitoStaticMocking.mockedConstructionClassId
)

val predefinedAutoCloseable: Set<ClassId> = mocksAutoCloseable

/**
 * Checks if this class is marked as auto closeable
 * (useful for classes that could not be loaded by class loader like mocks for mocking statics from Mockito Inline).
 */
internal val ClassId.isPredefinedAutoCloseable: Boolean
    get() = this in predefinedAutoCloseable

/**
 * Returns [AutoCloseable.close] method id for all auto closeable.
 * and predefined as auto closeable via [isPredefinedAutoCloseable], and null otherwise.
 * Null always for [BuiltinClassId].
 */
internal val ClassId.closeMethodIdOrNull: MethodId?
    get() = when {
        isPredefinedAutoCloseable -> closeMethodId
        this is BuiltinClassId -> null
        else -> (jClass as? AutoCloseable)?.let { closeMethodId }
    }
