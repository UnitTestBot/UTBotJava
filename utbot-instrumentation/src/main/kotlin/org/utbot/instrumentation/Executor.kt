package org.utbot.instrumentation

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KCallable

/**
 * Base interface for delegated execution logic.
 *
 * @param TResult the type of an execution result.
 */
interface Executor<TResult> {
    /**
     * Main method to override.
     * Returns the result of the execution of the [ExecutableId] with [arguments] and [parameters].
     *
     * @param arguments are additional data, e.g. static environment.
     */
    suspend fun executeAsync(
        kCallable: KCallable<*>,
        arguments: Array<Any?>,
        parameters: Any?
    ): TResult
}

fun <TResult> Executor<TResult>.execute(
    kCallable: KCallable<*>,
    arguments: Array<Any?>,
    parameters: Any? = null
) = runBlocking { executeAsync(kCallable, arguments, parameters) }



