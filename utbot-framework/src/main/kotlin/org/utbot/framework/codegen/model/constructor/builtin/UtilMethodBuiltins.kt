package org.utbot.framework.codegen.model.constructor.builtin

import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.model.constructor.util.utilMethodId
import org.utbot.framework.codegen.model.tree.CgClassId
import org.utbot.framework.codegen.utils.UtUtils
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import sun.misc.Unsafe
import kotlin.reflect.jvm.javaMethod

/**
 * Set of ids of all possible util methods for a given class
 * The class may actually not have some of these methods if they
 * are not required in the process of code generation
 */
internal abstract class UtilMethodProvider(val utilClassId: ClassId) {
    val utilMethodIds: Set<MethodId>
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

    abstract val getUnsafeInstanceMethodId: MethodId

    /**
     * Method that creates instance using Unsafe
     */
    abstract val createInstanceMethodId: MethodId

    abstract val createArrayMethodId: MethodId

    abstract val setFieldMethodId: MethodId

    abstract val setStaticFieldMethodId: MethodId

    abstract val getFieldValueMethodId: MethodId

    abstract val getStaticFieldValueMethodId: MethodId

    abstract val getEnumConstantByNameMethodId: MethodId

    abstract val deepEqualsMethodId: MethodId

    abstract val arraysDeepEqualsMethodId: MethodId

    abstract val iterablesDeepEqualsMethodId: MethodId

    abstract val streamsDeepEqualsMethodId: MethodId

    abstract val mapsDeepEqualsMethodId: MethodId

    abstract val hasCustomEqualsMethodId: MethodId

    abstract val getArrayLengthMethodId: MethodId
}

internal object LibraryUtilMethodProvider : UtilMethodProvider(UtUtils::class.id) {
    override val getUnsafeInstanceMethodId: MethodId = UtUtils::getUnsafeInstance.javaMethod!!.executableId

    override val createInstanceMethodId: MethodId = UtUtils::createInstance.javaMethod!!.executableId

    override val createArrayMethodId: MethodId = UtUtils::createArray.javaMethod!!.executableId

    override val setFieldMethodId: MethodId = UtUtils::setField.javaMethod!!.executableId

    override val setStaticFieldMethodId: MethodId = UtUtils::setStaticField.javaMethod!!.executableId

    override val getFieldValueMethodId: MethodId = UtUtils::getFieldValue.javaMethod!!.executableId

    override val getStaticFieldValueMethodId: MethodId = UtUtils::getStaticFieldValue.javaMethod!!.executableId

    override val getEnumConstantByNameMethodId: MethodId = UtUtils::getEnumConstantByName.javaMethod!!.executableId

    override val deepEqualsMethodId: MethodId = UtUtils::deepEquals.javaMethod!!.executableId

    override val arraysDeepEqualsMethodId: MethodId = UtUtils::arraysDeepEquals.javaMethod!!.executableId

    override val iterablesDeepEqualsMethodId: MethodId = UtUtils::iterablesDeepEquals.javaMethod!!.executableId

    override val streamsDeepEqualsMethodId: MethodId = UtUtils::streamsDeepEquals.javaMethod!!.executableId

    override val mapsDeepEqualsMethodId: MethodId = UtUtils::mapsDeepEquals.javaMethod!!.executableId

    override val hasCustomEqualsMethodId: MethodId = UtUtils::hasCustomEquals.javaMethod!!.executableId

    override val getArrayLengthMethodId: MethodId = UtUtils::getArrayLength.javaMethod!!.executableId

//    fun patterns(): Patterns {
//        return Patterns(
//            moduleLibraryPatterns = emptyList(),
//            libraryPatterns = listOf(CODEGEN_UTILS_JAR_PATTERN)
//        )
//    }
}

internal class TestClassUtilMethodProvider(testClassId: ClassId) : UtilMethodProvider(testClassId) {
    override val getUnsafeInstanceMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getUnsafeInstance",
            returnType = Unsafe::class.id,
        )

    override val createInstanceMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "createInstance",
            returnType = CgClassId(objectClassId, isNullable = true),
            arguments = arrayOf(stringClassId)
        )

    override val createArrayMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "createArray",
            returnType = Array<Any>::class.id,
            arguments = arrayOf(stringClassId, intClassId, Array<Any>::class.id)
        )

    override val setFieldMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "setField",
            returnType = voidClassId,
            arguments = arrayOf(objectClassId, stringClassId, objectClassId)
        )

    override val setStaticFieldMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "setStaticField",
            returnType = voidClassId,
            arguments = arrayOf(Class::class.id, stringClassId, objectClassId)
        )

    override val getFieldValueMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(objectClassId, stringClassId)
        )

    override val getStaticFieldValueMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getStaticFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
        )

    override val getEnumConstantByNameMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getEnumConstantByName",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
        )

    override val deepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "deepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId, objectClassId)
        )

    override val arraysDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "arraysDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId, objectClassId)
        )

    override val iterablesDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "iterablesDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.lang.Iterable::class.id, java.lang.Iterable::class.id)
        )

    override val streamsDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "streamsDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.util.stream.Stream::class.id, java.util.stream.Stream::class.id)
        )

    override val mapsDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "mapsDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.util.Map::class.id, java.util.Map::class.id)
        )

    override val hasCustomEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "hasCustomEquals",
            returnType = booleanClassId,
            arguments = arrayOf(Class::class.id)
        )

    override val getArrayLengthMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getArrayLength",
            returnType = intClassId,
            arguments = arrayOf(objectClassId)
        )

    //WARN: if you make changes in the following sets of exceptions,
    //don't forget to change them in hardcoded [UtilMethods] as well
    internal fun findExceptionTypesOf(methodId: MethodId): Set<ClassId> {
        if (methodId !in utilMethodIds) return emptySet()

        with(this) {
            return when (methodId) {
                getEnumConstantByNameMethodId -> setOf(java.lang.IllegalAccessException::class.id)
                getStaticFieldValueMethodId,
                getFieldValueMethodId,
                setStaticFieldMethodId,
                setFieldMethodId -> setOf(java.lang.IllegalAccessException::class.id, java.lang.NoSuchFieldException::class.id)
                createInstanceMethodId -> setOf(Exception::class.id)
                getUnsafeInstanceMethodId -> setOf(java.lang.ClassNotFoundException::class.id, java.lang.NoSuchFieldException::class.id, java.lang.IllegalAccessException::class.id)
                createArrayMethodId -> setOf(java.lang.ClassNotFoundException::class.id)
                deepEqualsMethodId,
                arraysDeepEqualsMethodId,
                iterablesDeepEqualsMethodId,
                streamsDeepEqualsMethodId,
                mapsDeepEqualsMethodId,
                hasCustomEqualsMethodId,
                getArrayLengthMethodId -> emptySet()
                else -> error("Unknown util method $this")
            }
        }
    }
}

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
