package org.utbot.framework.codegen.model.constructor.builtin

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import sun.misc.Unsafe
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

// reflection methods ids

//TODO: these methods are called builtins, but actually are just [MethodId]
//may be fixed in https://github.com/UnitTestBot/UTBotJava/issues/138

internal val reflectionBuiltins: Set<MethodId>
         get() = setOf(
                setAccessible, invoke, newInstance, getMethodId, forName,
                getDeclaredMethod, getDeclaredConstructor, allocateInstance,
                getClass, getDeclaredField, isEnumConstant, getFieldName,
                equals, getSuperclass, setMethodId, newArrayInstance,
                setArrayElement, getArrayElement, getTargetException,
        )

internal val setAccessible = methodId(
        classId = AccessibleObject::class.id,
        name = "setAccessible",
        returnType = voidClassId,
        arguments = arrayOf(booleanClassId)
)

internal val invoke = methodId(
        classId = java.lang.reflect.Method::class.id,
        name = "invoke",
        returnType = objectClassId,
        arguments = arrayOf(objectClassId, Array<Any>::class.id)
)

internal val newInstance = methodId(
        classId = java.lang.reflect.Constructor::class.id,
        name = "newInstance",
        returnType = objectClassId,
        arguments = arrayOf(Array<Any>::class.id)
)

internal val getMethodId = methodId(
        classId = Field::class.id,
        name = "get",
        returnType = objectClassId,
        arguments = arrayOf(objectClassId)
)

internal val forName = methodId(
        classId = Class::class.id,
        name = "forName",
        returnType = Class::class.id,
        arguments = arrayOf(stringClassId)
)

internal val getDeclaredMethod = methodId(
        classId = Class::class.id,
        name = "getDeclaredMethod",
        returnType = java.lang.reflect.Method::class.id,
        arguments = arrayOf(
                stringClassId,
                ClassId("[Ljava.lang.Class;", Class::class.id)
        )
)

internal val getDeclaredConstructor = methodId(
        classId = Class::class.id,
        name = "getDeclaredConstructor",
        returnType = java.lang.reflect.Constructor::class.id,
        arguments = arrayOf(ClassId("[Ljava.lang.Class;", Class::class.id))
)

internal val allocateInstance = methodId(
        classId = Unsafe::class.id,
        name = "allocateInstance",
        returnType = objectClassId,
        arguments = arrayOf(Class::class.id)
)

internal val getClass = methodId(
        classId = objectClassId,
        name = "getClass",
        returnType = Class::class.id
)

internal val getDeclaredField = methodId(
        classId = Class::class.id,
        name = "getDeclaredField",
        returnType = Field::class.id,
        arguments = arrayOf(stringClassId)
)

internal val getDeclaredFields = methodId(
        classId = Class::class.id,
        name = "getDeclaredFields",
        returnType = Array<Field>::class.id
)

internal val isEnumConstant = methodId(
        classId = java.lang.reflect.Field::class.id,
        name = "isEnumConstant",
        returnType = booleanClassId
)

internal val getFieldName = methodId(
        classId = java.lang.reflect.Field::class.id,
        name = "getName",
        returnType = stringClassId
)

// Object's equals() method
internal val equals = methodId(
        classId = objectClassId,
        name = "equals",
        returnType = booleanClassId,
        arguments = arrayOf(objectClassId)
)

internal val getSuperclass = methodId(
        classId = Class::class.id,
        name = "getSuperclass",
        returnType = Class::class.id
)

internal val setMethodId = methodId(
        classId = Field::class.id,
        name = "set",
        returnType = voidClassId,
        arguments = arrayOf(objectClassId, objectClassId)
)

internal val newArrayInstance = methodId(
        classId = java.lang.reflect.Array::class.id,
        name = "newInstance",
        returnType = objectClassId,
        arguments = arrayOf(java.lang.Class::class.id, intClassId)
)

internal val setArrayElement = methodId(
        classId = java.lang.reflect.Array::class.id,
        name = "set",
        returnType = voidClassId,
        arguments = arrayOf(objectClassId, intClassId, objectClassId)
)

internal val getArrayElement = methodId(
        classId = java.lang.reflect.Array::class.id,
        name = "get",
        returnType = objectClassId,
        arguments = arrayOf(objectClassId, intClassId)
)

internal val getTargetException = methodId(
        classId = InvocationTargetException::class.id,
        name = "getTargetException",
        returnType = java.lang.Throwable::class.id
)
