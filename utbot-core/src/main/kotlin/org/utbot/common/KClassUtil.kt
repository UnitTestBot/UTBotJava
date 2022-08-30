package org.utbot.common

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

val Class<*>.packageName: String get() = `package`?.name?:""

fun Method.invokeCatching(obj: Any?, args: List<Any?>) = try {
    Result.success(invoke(obj, *args.toTypedArray()))
} catch (e: InvocationTargetException) {
    Result.failure<Nothing>(e.targetException)
}

val KClass<*>.allNestedClasses: List<KClass<*>>
    get() = listOf(this) + nestedClasses.flatMap { it.allNestedClasses }
