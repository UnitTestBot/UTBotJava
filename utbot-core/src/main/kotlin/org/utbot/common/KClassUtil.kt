package org.utbot.common

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Invokes [this] method of passed [obj] instance (null for static methods) with the passed [args] arguments.
 * NOTE: vararg parameters must be passed as an array of the corresponding type.
 */
fun Method.invokeCatching(obj: Any?, args: List<Any?>) = try {
    val invocation = invoke(obj, *args.toTypedArray())

    Result.success(invocation)
} catch (e: InvocationTargetException) {
    Result.failure<Nothing>(e.targetException)
}