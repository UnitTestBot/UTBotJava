package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.*
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId
import org.utbot.jcdb.api.isPrimitive
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

class ReflectionContext {

    val ClassId.javaClass: Class<*>
        get() {
            return when {
                isPrimitive -> idToPrimitive[this]!!
                isArray -> Class.forName(name, true, utContext.classLoader) // TODO: probably rewrite
                else -> utContext.classLoader.loadClass(name)
            }
        }

    val FieldId.javaFieldOrNull: Field?
        get() = classId.javaClass.declaredFields.firstOrNull { it.name == name }

    val FieldId.javaField: Field
        get() = javaFieldOrNull ?: error("Field $name is not declared in class ${classId.name}")


    val ClassId.kClass: KClass<*>
        get() = javaClass.kotlin


    fun ClassId.findFieldOrNull(fieldId: FieldId): Field? {
        if (this blockingIsNotSubtypeOf fieldId.classId) {
            return null
        }

        return fieldId.javaField
    }

    val ConstructorExecutableId.constructor: Constructor<*>
        get() {
            val declaringClass = classId.javaClass
            return declaringClass.singleConstructorOrNull(signature)
                ?: error("Can't find method $signature in ${declaringClass.name}")
        }

    val MethodExecutableId.method: Method
        get() {
            val declaringClass = classId.javaClass
            return declaringClass.singleMethodOrNull(signature)
                ?: error("Can't find method $signature in ${declaringClass.name}")
        }

    val ExecutableId.executable: Executable
        get() = when (this) {
            is MethodExecutableId -> method
            is ConstructorExecutableId -> constructor
        }

}

private val defaultContext = ReflectionContext()

val reflection get() = if (true) defaultContext else throw IllegalStateException("reflection is disabled in current process")