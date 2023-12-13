package org.utbot.framework.utils

import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinConstructorId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.CgClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.arrayTypeOf
import org.utbot.framework.plugin.api.util.baseStreamClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.builtinConstructorId
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import sun.misc.Unsafe
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method

/**
 * Set of ids of all possible util methods for a given class.
 *
 * The class may actually not have some of these methods if they
 * are not required in the process of code generation (this is the case for [TestClassUtilMethodProvider]).
 */
abstract class UtilMethodProvider(val utilClassId: ClassId) {
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
            getArrayLengthMethodId,
            consumeBaseStreamMethodId,
            buildStaticLambdaMethodId,
            buildLambdaMethodId,
            getLookupInMethodId,
            getLambdaCapturedArgumentTypesMethodId,
            getLambdaCapturedArgumentValuesMethodId,
            getInstantiatedMethodTypeMethodId,
            getLambdaMethodMethodId,
            getSingleAbstractMethodMethodId
        )

    val getUnsafeInstanceMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getUnsafeInstance",
            returnType = Unsafe::class.id,
        )

    /**
     * Method that creates instance using Unsafe
     */
    val createInstanceMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "createInstance",
            returnType = CgClassId(objectClassId, isNullable = true),
            arguments = arrayOf(stringClassId)
        )

    val createArrayMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "createArray",
            returnType = Array<Any>::class.id,
            arguments = arrayOf(stringClassId, intClassId, Array<Any>::class.id)
        )

    val setFieldMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "setField",
            returnType = voidClassId,
            arguments = arrayOf(objectClassId, stringClassId, stringClassId, objectClassId)
        )

    val setStaticFieldMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "setStaticField",
            returnType = voidClassId,
            arguments = arrayOf(Class::class.id, stringClassId, objectClassId)
        )

    val getFieldValueMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(objectClassId, stringClassId, stringClassId)
        )

    val getStaticFieldValueMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getStaticFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
        )

    val getEnumConstantByNameMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getEnumConstantByName",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
        )

    val deepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "deepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId, objectClassId)
        )

    val arraysDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "arraysDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId, objectClassId)
        )

    val iterablesDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "iterablesDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.lang.Iterable::class.id, java.lang.Iterable::class.id)
        )

    val streamsDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "streamsDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.util.stream.BaseStream::class.id, java.util.stream.BaseStream::class.id)
        )

    val mapsDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "mapsDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.util.Map::class.id, java.util.Map::class.id)
        )

    val hasCustomEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "hasCustomEquals",
            returnType = booleanClassId,
            arguments = arrayOf(Class::class.id)
        )

    val getArrayLengthMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getArrayLength",
            returnType = intClassId,
            arguments = arrayOf(objectClassId)
        )

    val consumeBaseStreamMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "consumeBaseStream",
            returnType = voidClassId,
            arguments = arrayOf(baseStreamClassId)
        )

    val buildStaticLambdaMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "buildStaticLambda",
            returnType = objectClassId,
            arguments = arrayOf(
                classClassId,
                classClassId,
                stringClassId,
                arrayTypeOf(capturedArgumentClassId)
            )
        )

    val buildLambdaMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "buildLambda",
            returnType = objectClassId,
            arguments = arrayOf(
                classClassId,
                classClassId,
                stringClassId,
                objectClassId,
                arrayTypeOf(capturedArgumentClassId)
            )
        )

    val getLookupInMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLookupIn",
            returnType = MethodHandles.Lookup::class.id,
            arguments = arrayOf(classClassId)
        )

    val getLambdaCapturedArgumentTypesMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLambdaCapturedArgumentTypes",
            returnType = arrayTypeOf(classClassId),
            arguments = arrayOf(arrayTypeOf(capturedArgumentClassId))
        )

    val getLambdaCapturedArgumentValuesMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLambdaCapturedArgumentValues",
            returnType = objectArrayClassId,
            arguments = arrayOf(arrayTypeOf(capturedArgumentClassId))
        )

    val getInstantiatedMethodTypeMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getInstantiatedMethodType",
            returnType = MethodType::class.id,
            arguments = arrayOf(Method::class.id, arrayTypeOf(classClassId))
        )

    val getLambdaMethodMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLambdaMethod",
            returnType = Method::class.id,
            arguments = arrayOf(classClassId, stringClassId)
        )

    val getSingleAbstractMethodMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getSingleAbstractMethod",
            returnType = java.lang.reflect.Method::class.id,
            arguments = arrayOf(classClassId)
        )

    val capturedArgumentClassId: BuiltinClassId
        get() = BuiltinClassId(
            canonicalName = "${utilClassId.name}.CapturedArgument",
            simpleName = "CapturedArgument"
        )

    val capturedArgumentConstructorId: BuiltinConstructorId
        get() = builtinConstructorId(capturedArgumentClassId, classClassId, objectClassId)
}

internal fun ClassId.utilMethodId(
    name: String,
    returnType: ClassId,
    vararg arguments: ClassId,
    // usually util methods are static, so this argument is true by default
    isStatic: Boolean = true
): MethodId =
    BuiltinMethodId(this, name, returnType, arguments.toList(), isStatic = isStatic)
