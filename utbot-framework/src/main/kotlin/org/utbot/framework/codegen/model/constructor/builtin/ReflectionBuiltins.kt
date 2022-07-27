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

internal val setAccessible get() = methodId(
        classId = AccessibleObject::class.id,
        name = "setAccessible",
        returnType = voidClassId,
        arguments = arrayOf(booleanClassId)
)

internal val invoke get() = methodId(
        classId = java.lang.reflect.Method::class.id,
        name = "invoke",
        returnType = objectClassId,
        arguments = arrayOf(objectClassId, Array<Any>::class.id)
)

internal val newInstance get() = methodId(
        classId = java.lang.reflect.Constructor::class.id,
        name = "newInstance",
        returnType = objectClassId,
        arguments = arrayOf(Array<Any>::class.id)
)

internal val get get() = methodId(
        classId = Field::class.id,
        name = "get",
        returnType = objectClassId,
        arguments = arrayOf(objectClassId)
)

internal val forName get() = methodId(
        classId = Class::class.id,
        name = "forName",
        returnType = Class::class.id,
        arguments = arrayOf(stringClassId)
)

internal val getDeclaredMethod get() = methodId(
        classId = Class::class.id,
        name = "getDeclaredMethod",
        returnType = java.lang.reflect.Method::class.id,
        arguments = arrayOf(
                stringClassId,
                arrayOfClasses
        )
)

internal val getDeclaredConstructor get() = methodId(
        classId = Class::class.id,
        name = "getDeclaredConstructor",
        returnType = java.lang.reflect.Constructor::class.id,
        arguments = arrayOf(arrayOfClasses)
)

internal val allocateInstance get() = methodId(
        classId = Unsafe::class.id,
        name = "allocateInstance",
        returnType = objectClassId,
        arguments = arrayOf(Class::class.id)
)

internal val getClass get() = methodId(
        classId = objectClassId,
        name = "getClass",
        returnType = Class::class.id
)

internal val getDeclaredField get() = methodId(
        classId = Class::class.id,
        name = "getDeclaredField",
        returnType = Field::class.id,
        arguments = arrayOf(stringClassId)
)

internal val getDeclaredFields get() = methodId(
        classId = Class::class.id,
        name = "getDeclaredFields",
        returnType = Array<Field>::class.id
)

internal val isEnumConstant get() = methodId(
        classId = java.lang.reflect.Field::class.id,
        name = "isEnumConstant",
        returnType = booleanClassId
)

internal val getFieldName get()  = methodId(
        classId = java.lang.reflect.Field::class.id,
        name = "getName",
        returnType = stringClassId
)

// Object's equals() method
internal val equals get() = methodId(
        classId = objectClassId,
        name = "equals",
        returnType = booleanClassId,
        arguments = arrayOf(objectClassId)
)

internal val getSuperclass get() = methodId(
        classId = Class::class.id,
        name = "getSuperclass",
        returnType = Class::class.id
)

internal val set get() = methodId(
        classId = Field::class.id,
        name = "set",
        returnType = voidClassId,
        arguments = arrayOf(objectClassId, objectClassId)
)

internal val newArrayInstance get()  = methodId(
        classId = java.lang.reflect.Array::class.id,
        name = "newInstance",
        returnType = objectClassId,
        arguments = arrayOf(java.lang.Class::class.id, intClassId)
)

internal val setArrayElement get() = methodId(
        classId = java.lang.reflect.Array::class.id,
        name = "set",
        returnType = voidClassId,
        arguments = arrayOf(objectClassId, intClassId, objectClassId)
)

internal val getArrayElement get() = methodId(
        classId = java.lang.reflect.Array::class.id,
        name = "get",
        returnType = objectClassId,
        arguments = arrayOf(objectClassId, intClassId)
)

internal val getTargetException get() = methodId(
        classId = InvocationTargetException::class.id,
        name = "getTargetException",
        returnType = java.lang.Throwable::class.id
)
