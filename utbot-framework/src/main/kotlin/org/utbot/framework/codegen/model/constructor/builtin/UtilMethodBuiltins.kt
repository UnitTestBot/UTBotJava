package org.utbot.framework.codegen.model.constructor.builtin

import kotlinx.coroutines.runBlocking
import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.util.*
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.MethodId
import org.utbot.jcdb.api.isSubtypeOf
import sun.misc.Unsafe

/**
 * Set of ids of all possible util methods for a given class
 * The class may actually not have some of these methods if they
 * are not required in the process of code generation
 */
internal val BuiltinClassId.possibleUtilMethodIds: Set<MethodId>
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

internal val BuiltinClassId.getUnsafeInstanceMethodId: MethodId
    get() = newBuiltinMethod(
            name = "getUnsafeInstance",
            returnType = Unsafe::class.id,
    )

/**
 * Method that creates instance using Unsafe
 */
internal val BuiltinClassId.createInstanceMethodId: MethodId
    get() = newBuiltinMethod(
            name = "createInstance",
            returnType = objectClassId,
            arguments = listOf(stringClassId)
    )

internal val BuiltinClassId.createArrayMethodId: MethodId
    get() = newBuiltinMethod(
            name = "createArray",
            returnType = Array<Any>::class.id,
            arguments = listOf(stringClassId, intClassId, Array<Any>::class.id)
    )

internal val BuiltinClassId.setFieldMethodId: MethodId
    get() = newBuiltinMethod(
            name = "setField",
            returnType = voidClassId,
            arguments = listOf(objectClassId, stringClassId, objectClassId)
    )

internal val BuiltinClassId.setStaticFieldMethodId: MethodId
    get() = newBuiltinMethod(
            name = "setStaticField",
            returnType = voidClassId,
            arguments = listOf(Class::class.id, stringClassId, objectClassId)
    )

internal val BuiltinClassId.getFieldValueMethodId: MethodId
    get() = newBuiltinMethod(
            name = "getFieldValue",
            returnType = objectClassId,
            arguments = listOf(objectClassId, stringClassId)
    )

internal val BuiltinClassId.getStaticFieldValueMethodId: MethodId
    get() = newBuiltinMethod(
            name = "getStaticFieldValue",
            returnType = objectClassId,
            arguments = listOf(Class::class.id, stringClassId)
    )

internal val BuiltinClassId.getEnumConstantByNameMethodId: MethodId
    get() = newBuiltinMethod(
            name = "getEnumConstantByName",
            returnType = objectClassId,
            arguments = listOf(Class::class.id, stringClassId)
    )

internal val BuiltinClassId.deepEqualsMethodId: MethodId
    get() = newBuiltinMethod(
        name = "deepEquals",
        returnType = booleanClassId,
        arguments = listOf(objectClassId, objectClassId)
    )

internal val BuiltinClassId.arraysDeepEqualsMethodId: MethodId
    get() = newBuiltinMethod(
        name = "arraysDeepEquals",
        returnType = booleanClassId,
        arguments = listOf(objectClassId, objectClassId)
    )

internal val BuiltinClassId.iterablesDeepEqualsMethodId: MethodId
    get() = newBuiltinMethod(
        name = "iterablesDeepEquals",
        returnType = booleanClassId,
        arguments = listOf(java.lang.Iterable::class.id, java.lang.Iterable::class.id)
    )

internal val BuiltinClassId.streamsDeepEqualsMethodId: MethodId
    get() = newBuiltinMethod(
        name = "streamsDeepEquals",
        returnType = booleanClassId,
        arguments = listOf(java.util.stream.Stream::class.id, java.util.stream.Stream::class.id)
    )

internal val BuiltinClassId.mapsDeepEqualsMethodId: MethodId
    get() = newBuiltinMethod(
        name = "mapsDeepEquals",
        returnType = booleanClassId,
        arguments = listOf(java.util.Map::class.id, java.util.Map::class.id)
    )

internal val BuiltinClassId.hasCustomEqualsMethodId: MethodId
    get() = newBuiltinMethod(
        name = "hasCustomEquals",
        returnType = booleanClassId,
        arguments = listOf(Class::class.id)
    )

internal val BuiltinClassId.getArrayLengthMethodId: MethodId
    get() = newBuiltinMethod(
        name = "getArrayLength",
        returnType = intClassId,
        arguments = listOf(objectClassId)
    )

/**
 * [MethodId] for [AutoCloseable.close].
 */
val closeMethodId get() = AutoCloseable::class.java.id.findMethod(
    name = "close",
    returnType = voidClassId
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
    get() = runBlocking {
        when {
            isPredefinedAutoCloseable -> closeMethodId
            this@closeMethodIdOrNull is BuiltinClassId -> null
            this@closeMethodIdOrNull isSubtypeOf asClass<AutoCloseable>() -> closeMethodId
            else -> null
        }
    }
