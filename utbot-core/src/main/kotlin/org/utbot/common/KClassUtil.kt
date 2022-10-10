package org.utbot.common

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

val Class<*>.nameOfPackage: String get() = `package`?.name?:""

fun Method.invokeCatching(obj: Any?, args: List<Any?>) = try {
    Result.success(invoke(obj, *args.toTypedArray()))
} catch (e: InvocationTargetException) {
    Result.failure<Nothing>(e.targetException)
}

val Class<*>.allNestedClasses: List<Class<*>>
    get() = listOf(this) + this.declaredClasses.flatMap { it.allNestedClasses.toList() }