package org.utbot.rd.loggers

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KLogger
import org.utbot.common.LoggerWithLogMethod


fun Logger.withLevel(logLevel: LogLevel): LoggerWithLogMethod = LoggerWithLogMethod {
    this.log(logLevel, it)
}

fun Logger.info(): LoggerWithLogMethod = LoggerWithLogMethod {
    this.info(it)
}
fun Logger.debug(): LoggerWithLogMethod = LoggerWithLogMethod {
    this.debug(it)
}
fun Logger.trace(): LoggerWithLogMethod = LoggerWithLogMethod {
    this.trace(it)
}

fun overrideDefaultRdLoggerFactoryWithKLogger(logger: KLogger) {
    if (Statics<ILoggerFactory>().get() !is UtRdKLoggerFactory) {
        Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))
    }
}