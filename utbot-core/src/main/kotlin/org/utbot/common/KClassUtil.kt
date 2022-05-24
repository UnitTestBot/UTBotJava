package org.utbot.common

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


val Class<*>.packageName: String get() = `package`?.name?:""

fun Class<*>.findField(name: String): Field =
    findFieldOrNull(name) ?: error("Can't find field $name in $this")

fun Class<*>.findFieldOrNull(name: String): Field? = generateSequence(this) { it.superclass }
    .mapNotNull {
        try {
            it.getField(name)
        } catch (e: NoSuchFieldException) {
            try {
                it.getDeclaredField(name)
            } catch (e: NoSuchFieldException) {
                null
            }
        }
    }
    .firstOrNull()

fun Method.invokeCatching(obj: Any?, args: List<Any?>) = try {
    Result.success(invoke(obj, *args.toTypedArray()))
} catch (e: InvocationTargetException) {
    Result.failure<Nothing>(e.targetException)
}
