package org.utbot.rd.loggers

import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.defaultLogFormat
import mu.KLogger
import org.utbot.common.dateFormatter
import java.time.LocalDateTime

class UtRdKLogger(private val realLogger: KLogger, private val category: String): Logger {
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

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        if (!isEnabled(level))
            return

        val msg = LocalDateTime.now().format(dateFormatter) + " | ${defaultLogFormat(category, level, message, throwable)}"

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