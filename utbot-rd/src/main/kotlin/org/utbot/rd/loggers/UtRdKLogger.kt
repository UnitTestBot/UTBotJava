package org.utbot.rd.loggers

import com.jetbrains.rd.util.*
import mu.KLogger

/**
 * Adapter from RD Logger to KLogger
 */
class UtRdKLogger(
    private val realLogger: KLogger,
    val category: String,
    private val logTraceOnError: Boolean = true
) : Logger {
    val logLevel: LogLevel
        get() {
            return when {
                realLogger.isTraceEnabled -> LogLevel.Trace
                realLogger.isDebugEnabled -> LogLevel.Debug
                realLogger.isInfoEnabled -> LogLevel.Info
                realLogger.isWarnEnabled -> LogLevel.Warn
                realLogger.isErrorEnabled -> LogLevel.Error
                else -> LogLevel.Fatal
            }
        }

    override fun isEnabled(level: LogLevel): Boolean {
        return level >= logLevel
    }

    private fun format(level: LogLevel, message: Any?, throwable: Throwable?): String {
        val throwableToPrint =
            if (logTraceOnError && level >= LogLevel.Error)
                throwable ?: Exception("No exception was actually thrown, this exception is used purely to log trace")
            else
                throwable
        val rdCategory = if (category.isNotEmpty()) "RdCategory: ${category.substringAfterLast('.').padEnd(25)} | " else ""
        return "$rdCategory${message?.toString() ?: ""} ${throwableToPrint?.getThrowableText()?.let { "| $it" } ?: ""}"
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        if (!isEnabled(level) || (message == null && throwable == null))
            return

        val msg = format(level, message, throwable)

        when (level) {
            LogLevel.Trace -> realLogger.trace(msg)
            LogLevel.Debug -> realLogger.debug(msg)
            LogLevel.Info -> realLogger.info(msg)
            LogLevel.Warn -> realLogger.warn(msg)
            LogLevel.Error -> realLogger.error(msg)
            LogLevel.Fatal -> realLogger.error(msg)
        }
    }
}