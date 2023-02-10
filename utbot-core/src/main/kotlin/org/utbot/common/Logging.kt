package org.utbot.common

import mu.KLogger
import java.time.format.DateTimeFormatter

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss")

class LoggerWithLogMethod(val logMethod: (() -> Any?) -> Unit)

fun KLogger.info(): LoggerWithLogMethod = LoggerWithLogMethod(this::info)
fun KLogger.debug(): LoggerWithLogMethod = LoggerWithLogMethod(this::debug)
fun KLogger.trace(): LoggerWithLogMethod = LoggerWithLogMethod(this::trace)

fun elapsedSecFrom(startNano: Long): String {
    val elapsedNano = System.nanoTime() - startNano
    val elapsedS = elapsedNano.toDouble() / 1_000_000_000
    return String.format("%.3f", elapsedS) + " sec"
}

/**
 * Structured logging.
 *
 * Usage: `logger.info().bracket("message") { ... block_of_code ...}`
 *
 * Results in
 *
 * ```
 * Started: message
 * -- other messages inside block of code
 * Finished: message
 * ```
 *
 * Method can handle exceptions and non local returns from inline lambda
 * You can use [closingComment] to add some result-depending comment to "Finished:" message. Special "<Nothing>" comment
 * is added if non local return happened in [block]
 */
inline fun <T> LoggerWithLogMethod.measureTime(
    crossinline msgBlock: () -> String,
    crossinline closingComment: (Result<T>) -> Any? = { "" },
    block: () -> T
): T {
    var msg = ""

    logMethod {
        msg = msgBlock()
        "Started: $msg"
    }

    val startNano = System.nanoTime()
    var alreadyLogged = false

    var res: Maybe<T> = Maybe.empty()
    try {
        // Note: don't replace this one with runCatching, otherwise return from lambda breaks "finished" logging.
        res = Maybe(block())
        return res.getOrThrow()
    } catch (t: Throwable) {
        logMethod { "Finished (in ${elapsedSecFrom(startNano)}): $msg :: EXCEPTION :: ${closingComment(Result.failure(t))}" }
        alreadyLogged = true
        throw t
    } finally {
        if (!alreadyLogged) {
            if (res.hasValue)
                logMethod { "Finished (in ${elapsedSecFrom(startNano)}): $msg ${closingComment(Result.success(res.getOrThrow()))}" }
            else
                logMethod { "Finished (in ${elapsedSecFrom(startNano)}): $msg <Nothing>" }
        }
    }
}

inline fun <T> KLogger.catchException(message: String = "Isolated", block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        this.error(message, e)
        null
    }
}

inline fun <T> KLogger.logException(message: String = "Exception occurred", block: () -> T): T {
    return try {
        block()
    } catch (e: Throwable) {
        this.error(message, e)
        throw e
    }
}

inline fun <T> silent(block: () -> T): T? {
    return try {
        block()
    } catch (_: Throwable) {
        null
    }
}