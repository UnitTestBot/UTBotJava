package org.utbot.spring.loggers

import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.getLogger
import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.LogFactoryImpl

@Suppress("unused") // used via -Dorg.apache.commons.logging.LogFactory=org.utbot.spring.loggers.RDApacheCommonsLogFactory
class RDApacheCommonsLogFactory : LogFactoryImpl() {
    override fun getInstance(name: String): Log {
        val logger = getLogger(category = name)
        return object : Log {
            override fun trace(message: Any?) = logger.log(LogLevel.Trace, message, throwable = null)
            override fun trace(message: Any?, t: Throwable?) = logger.log(LogLevel.Trace, message, throwable = t)
            override fun debug(message: Any?) = logger.log(LogLevel.Debug, message, throwable = null)
            override fun debug(message: Any?, t: Throwable?) = logger.log(LogLevel.Debug, message, throwable = t)
            override fun info(message: Any?) = logger.log(LogLevel.Info, message, throwable = null)
            override fun info(message: Any?, t: Throwable?) = logger.log(LogLevel.Info, message, throwable = t)
            override fun warn(message: Any?) = logger.log(LogLevel.Warn, message, throwable = null)
            override fun warn(message: Any?, t: Throwable?) = logger.log(LogLevel.Warn, message, throwable = t)
            override fun error(message: Any?) = logger.log(LogLevel.Error, message, throwable = null)
            override fun error(message: Any?, t: Throwable?) = logger.log(LogLevel.Error, message, throwable = t)
            override fun fatal(message: Any?) = logger.log(LogLevel.Fatal, message, throwable = null)
            override fun fatal(message: Any?, t: Throwable?) = logger.log(LogLevel.Fatal, message, throwable = t)

            override fun isTraceEnabled() = logger.isEnabled(LogLevel.Trace)
            override fun isDebugEnabled() = logger.isEnabled(LogLevel.Debug)
            override fun isInfoEnabled() = logger.isEnabled(LogLevel.Info)
            override fun isErrorEnabled() = logger.isEnabled(LogLevel.Error)
            override fun isFatalEnabled() = logger.isEnabled(LogLevel.Fatal)
            override fun isWarnEnabled() = logger.isEnabled(LogLevel.Warn)
        }
    }
}