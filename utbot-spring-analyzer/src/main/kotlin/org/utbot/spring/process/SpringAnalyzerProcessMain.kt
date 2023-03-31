package org.utbot.spring.process

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KotlinLogging
import org.utbot.common.AbstractSettings
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.IdleWatchdog
import org.utbot.rd.RdSettingsContainerFactory
import org.utbot.rd.findRdPort
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.spring.analyzers.SpringApplicationAnalyzer
import org.utbot.spring.process.generated.SpringAnalyzerProcessModel
import org.utbot.spring.process.generated.SpringAnalyzerResult
import org.utbot.spring.process.generated.springAnalyzerProcessModel
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = KotlinLogging.logger {}

@Suppress("unused")
object SpringAnalyzerProcessMain

suspend fun main(args: Array<String>) {
    Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

    logger.info("-----------------------------------------------------------------------")
    logger.info("------------------NEW SPRING ANALYZER PROCESS STARTED------------------")
    logger.info("-----------------------------------------------------------------------")
    // 0 - auto port for server, should not be used here
    val port = findRdPort(args)


    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(port) {
        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol.settingsModel))
        springAnalyzerProcessModel.setup(it, protocol)
    }
}

private fun SpringAnalyzerProcessModel.setup(watchdog: IdleWatchdog, realProtocol: IProtocol) {
    watchdog.measureTimeForActiveCall(analyze, "Analyzing Spring Application") { params ->
        SpringAnalyzerResult(
            SpringApplicationAnalyzer(
                params.classpath.toList().map { File(it).toURI().toURL() },
                params.configuration,
                params.propertyFilesPaths.toList(),
                params.xmlConfigurationPaths.toList()
            ).analyze().toTypedArray()
        )
    }
}
