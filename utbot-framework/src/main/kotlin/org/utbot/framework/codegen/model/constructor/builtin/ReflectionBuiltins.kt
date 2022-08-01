package org.utbot.framework.codegen.model.constructor.builtin

import kotlinx.coroutines.runBlocking
import org.utbot.framework.plugin.api.util.*
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.MethodId
import sun.misc.Unsafe
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

// reflection methods ids

//TODO: these methods are called builtins, but actually are just [MethodId]
//may be fixed in https://github.com/UnitTestBot/UTBotJava/issues/138

internal val reflectionBuiltins: Set<MethodId>
         get() = setOf(
                setAccessible, invoke, newInstance, get, forName,
                getDeclaredMethod, getDeclaredConstructor, allocateInstance,
                getClass, getDeclaredField, isEnumConstant, getFieldName,
                equals, getSuperclass, set, newArrayInstance,
                setArrayElement, getArrayElement, getTargetException,
        )

internal val arrayOfClasses: ClassId get() = runBlocking { 
        utContext.classpath.findClassOrNull("java.lang.Class[]")!!
} 

internal val setAccessible get() = AccessibleObject::class.id.findMethod(
        name = "setAccessible",
        returnType = voidClassId,
        arguments = listOf(booleanClassId)
)

internal val invoke get() = java.lang.reflect.Method::class.id.findMethod(
        name = "invoke",
        returnType = objectClassId,
        arguments = listOf(objectClassId, Array<Any>::class.id)
)

internal val newInstance get() = java.lang.reflect.Constructor::class.id.findMethod(
        name = "newInstance",
        returnType = objectClassId,
        arguments = listOf(Array<Any>::class.id)
)

internal val get get() = Field::class.id.findMethod(
        name = "get",
        returnType = objectClassId,
        arguments = listOf(objectClassId)
)

internal val forName get() = Class::class.id.findMethod(
        name = "forName",
        returnType = Class::class.id,
        arguments = listOf(stringClassId)
)

internal val getDeclaredMethod get() = Class::class.id.findMethod(
        name = "getDeclaredMethod",
        returnType = java.lang.reflect.Method::class.id,
        arguments = listOf(
                stringClassId,
                arrayOfClasses
    )
)

internal val getDeclaredConstructor get() = Class::class.id.findMethod(
        name = "getDeclaredConstructor",
        returnType = java.lang.reflect.Constructor::class.id,
        arguments = listOf(arrayOfClasses)
)

internal val allocateInstance get() = Unsafe::class.id.findMethod(
        name = "allocateInstance",
        returnType = objectClassId,
        arguments = listOf(Class::class.id)
)

internal val getClass get() = objectClassId.findMethod(
        name = "getClass",
        returnType = Class::class.id
)

internal val getDeclaredField get() = Class::class.id.findMethod(
        name = "getDeclaredField",
        returnType = Field::class.id,
        arguments = listOf(stringClassId)
)

internal val getDeclaredFields get() = Class::class.id.findMethod(
        name = "getDeclaredFields",
        returnType = Array<Field>::class.id
)

internal val isEnumConstant get() = java.lang.reflect.Field::class.id.findMethod(
        name = "isEnumConstant",
        returnType = booleanClassId
)

internal val getFieldName get()  = java.lang.reflect.Field::class.id.findMethod(
        name = "getName",
        returnType = stringClassId
)

// Object's equals() method
internal val equals get() = objectClassId.findMethod(
        name = "equals",
        returnType = booleanClassId,
        arguments = listOf(objectClassId)
)

internal val getSuperclass get() = Class::class.id.findMethod(
        name = "getSuperclass",
        returnType = Class::class.id
)

internal val set get() = Field::class.id.findMethod(
        name = "set",
        returnType = voidClassId,
        arguments = listOf(objectClassId, objectClassId)
)

internal val newArrayInstance get()  = java.lang.reflect.Array::class.id.findMethod(
        name = "newInstance",
        returnType = objectClassId,
        arguments = listOf(java.lang.Class::class.id, intClassId)
)

internal val setArrayElement get() = java.lang.reflect.Array::class.id.findMethod(
        name = "set",
        returnType = voidClassId,
        arguments = listOf(objectClassId, intClassId, objectClassId)
)

internal val getArrayElement get() = java.lang.reflect.Array::class.id.findMethod(
        name = "get",
        returnType = objectClassId,
        arguments = listOf(objectClassId, intClassId)
)

internal val getTargetException get() = InvocationTargetException::class.id.findMethod(
        name = "getTargetException",
        returnType = java.lang.Throwable::class.id
)
