package org.utbot.spring.loggers

import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.LogFactoryImpl
import org.utbot.spring.exception.UtBotSpringShutdownException

@Suppress("unused") // used via -Dorg.apache.commons.logging.LogFactory=org.utbot.spring.loggers.RDApacheCommonsLogFactory
class RDApacheCommonsLogFactory : LogFactoryImpl() {
    override fun getInstance(name: String): Log {
        val logger = getLogger(category = name)
        return object : Log {
            private fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
                if (throwable is UtBotSpringShutdownException) {
                    // avoid polluting logs with stack trace of expected exception
                    logger.info { message }
                    logger.info { "${throwable::class.java.name}: ${throwable.message}" }
                } else
                    logger.log(level, message, throwable)
            }
            private fun isEnabled(level: LogLevel) = logger.isEnabled(level)

            override fun trace(message: Any?) = log(LogLevel.Trace, message, throwable = null)
            override fun trace(message: Any?, t: Throwable?) = log(LogLevel.Trace, message, throwable = t)
            override fun debug(message: Any?) = log(LogLevel.Debug, message, throwable = null)
            override fun debug(message: Any?, t: Throwable?) = log(LogLevel.Debug, message, throwable = t)
            override fun info(message: Any?) = log(LogLevel.Info, message, throwable = null)
            override fun info(message: Any?, t: Throwable?) = log(LogLevel.Info, message, throwable = t)
            override fun warn(message: Any?) = log(LogLevel.Warn, message, throwable = null)
            override fun warn(message: Any?, t: Throwable?) = log(LogLevel.Warn, message, throwable = t)
            override fun error(message: Any?) = log(LogLevel.Error, message, throwable = null)
            override fun error(message: Any?, t: Throwable?) = log(LogLevel.Error, message, throwable = t)
            override fun fatal(message: Any?) = log(LogLevel.Fatal, message, throwable = null)
            override fun fatal(message: Any?, t: Throwable?) = log(LogLevel.Fatal, message, throwable = t)

            override fun isTraceEnabled() = isEnabled(LogLevel.Trace)
            override fun isDebugEnabled() = isEnabled(LogLevel.Debug)
            override fun isInfoEnabled() = isEnabled(LogLevel.Info)
            override fun isErrorEnabled() = isEnabled(LogLevel.Error)
            override fun isFatalEnabled() = isEnabled(LogLevel.Fatal)
            override fun isWarnEnabled() = isEnabled(LogLevel.Warn)
        }
    }
}