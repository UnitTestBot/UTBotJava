package org.utbot.common

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


val Class<*>.packageName: String get() = `package`?.name?:""

/**
 * Returns the Field that would be used by compiler when you try to direct access [fieldName] from code, i.e.
 * when you write `${this.name}.$fieldName`.
 * If there is no field named [fieldName] in the class, null is returned
 */
fun Class<*>.findDirectAccessedFieldOrNull(fieldName: String): Field? = generateSequence(this) { it.superclass }
    .flatMap { it.declaredFields.asSequence() }
    .firstOrNull { it.name == fieldName }

fun Method.invokeCatching(obj: Any?, args: List<Any?>) = try {
    Result.success(invoke(obj, *args.toTypedArray()))
} catch (e: InvocationTargetException) {
    Result.failure<Nothing>(e.targetException)
}
