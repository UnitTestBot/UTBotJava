package org.utbot.rd

import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.defaultLogFormat
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object UtRdLoggerFactory : ILoggerFactory {
    override fun getLogger(category: String): Logger {
        logger.trace { "getting logger for category: $category" }
        return UtRdLogger(category)
    }
}

class UtRdLogger(private val category: String) : Logger {
    override fun isEnabled(level: LogLevel): Boolean {
        return when (level) {
            LogLevel.Trace -> logger.isTraceEnabled
            LogLevel.Debug -> logger.isDebugEnabled
            LogLevel.Info -> logger.isInfoEnabled
            LogLevel.Warn -> logger.isWarnEnabled
            LogLevel.Error -> logger.isErrorEnabled
            LogLevel.Fatal -> logger.isErrorEnabled
        }
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        val msg = defaultLogFormat(category, level, message, throwable)
        when (level) {
            LogLevel.Trace -> logger.trace(msg)
            LogLevel.Debug -> logger.debug(msg)
            LogLevel.Info -> logger.info(msg)
            LogLevel.Warn -> logger.warn(msg)
            LogLevel.Error -> logger.error(msg)
            LogLevel.Fatal -> logger.error(msg)
        }
    }
}