package org.utbot.rd.loggers

import com.jetbrains.rd.util.*
import mu.KLogger

class UtRdKLogger(private val realLogger: KLogger): Logger {
    override fun isEnabled(level: LogLevel): Boolean {
        return when (level) {
            LogLevel.Trace -> realLogger.isTraceEnabled
            LogLevel.Debug -> realLogger.isDebugEnabled
            LogLevel.Info -> realLogger.isInfoEnabled
            LogLevel.Warn -> realLogger.isWarnEnabled
            LogLevel.Error -> realLogger.isErrorEnabled
            LogLevel.Fatal -> realLogger.isErrorEnabled
        }
    }

    private fun format(level: LogLevel, message: Any?, throwable: Throwable?): String {
        val throwableToPrint = if (level < LogLevel.Error) throwable else throwable ?: Exception() //to print stacktrace
        return "${message?.toString() ?: ""} ${throwableToPrint?.getThrowableText()?.let { "| $it" } ?: ""}"
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        if (!isEnabled(level))
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