package org.utbot.rd.loggers

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KLogger
import org.utbot.common.LoggerWithLogMethod
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.generated.synchronizationModel


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

// TODO use in instrumented process
fun setupRdLogger(rdProcess: ProcessWithRdServer, loggerModel: LoggerModel, rdLogger: UtRdKLogger) {
    // currently we do not specify log level for different categories
    // though it is possible with some additional map on categories -> consider performance
    loggerModel.getCategoryMinimalLogLevel.set { _ ->
        // this logLevel is obtained from KotlinLogger
        rdLogger.logLevel.ordinal
    }

    loggerModel.log.advise(rdProcess.lifetime) {
        val logLevel = UtRdRemoteLogger.logLevelValues[it.logLevelOrdinal]
        // assume throwable already in message
        rdLogger.log(logLevel, it.message, null)
    }

    rdProcess.protocol.synchronizationModel.initRemoteLogging.fire(Unit)
}
