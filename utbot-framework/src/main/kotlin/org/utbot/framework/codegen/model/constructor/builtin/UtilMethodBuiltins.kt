package org.utbot.framework.codegen.model.constructor.builtin

import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.utils.UtUtils
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.voidClassId
import kotlin.reflect.jvm.javaMethod

/**
 * Set of ids of all possible util methods for a given class
 * The class may actually not have some of these methods if they
 * are not required in the process of code generation
 */
internal val utilMethodIds: Set<MethodId>
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

/**
 * ClassId of [org.utbot.framework.codegen.model.util.UtUtils] class.
 */
internal val utUtilsClassId: ClassId = UtUtils::class.id

internal val getUnsafeInstanceMethodId: MethodId = UtUtils::getUnsafeInstance.javaMethod!!.executableId

/**
 * Method that creates instance using Unsafe
 */
internal val createInstanceMethodId: MethodId = UtUtils::createInstance.javaMethod!!.executableId

internal val createArrayMethodId: MethodId = UtUtils::createArray.javaMethod!!.executableId

internal val setFieldMethodId: MethodId = UtUtils::setField.javaMethod!!.executableId

internal val setStaticFieldMethodId: MethodId = UtUtils::setStaticField.javaMethod!!.executableId

internal val getFieldValueMethodId: MethodId = UtUtils::getFieldValue.javaMethod!!.executableId

internal val getStaticFieldValueMethodId: MethodId = UtUtils::getStaticFieldValue.javaMethod!!.executableId

internal val getEnumConstantByNameMethodId: MethodId = UtUtils::getEnumConstantByName.javaMethod!!.executableId

internal val deepEqualsMethodId: MethodId = UtUtils::deepEquals.javaMethod!!.executableId

internal val arraysDeepEqualsMethodId: MethodId = UtUtils::arraysDeepEquals.javaMethod!!.executableId

internal val iterablesDeepEqualsMethodId: MethodId = UtUtils::iterablesDeepEquals.javaMethod!!.executableId

internal val streamsDeepEqualsMethodId: MethodId = UtUtils::streamsDeepEquals.javaMethod!!.executableId

internal val mapsDeepEqualsMethodId: MethodId = UtUtils::mapsDeepEquals.javaMethod!!.executableId

internal val hasCustomEqualsMethodId: MethodId = UtUtils::hasCustomEquals.javaMethod!!.executableId

internal val getArrayLengthMethodId: MethodId = UtUtils::getArrayLength.javaMethod!!.executableId

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
