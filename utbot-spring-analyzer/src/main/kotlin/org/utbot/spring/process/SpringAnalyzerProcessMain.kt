package org.utbot.spring.process

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import org.utbot.common.AbstractSettings
import org.utbot.common.silentlyCloseStandardStreams
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.IdleWatchdog
import org.utbot.rd.RdSettingsContainerFactory
import org.utbot.rd.findRdPort
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.UtRdRemoteLoggerFactory
import org.utbot.spring.analyzers.SpringApplicationAnalyzer
import org.utbot.spring.data.ApplicationData
import org.utbot.spring.process.generated.SpringAnalyzerProcessModel
import org.utbot.spring.process.generated.SpringAnalyzerResult
import org.utbot.spring.process.generated.springAnalyzerProcessModel
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = getLogger<SpringAnalyzerProcessMain>()

@Suppress("unused")
object SpringAnalyzerProcessMain

suspend fun main(args: Array<String>) {
    // We don't want user code to litter the standard output, so we redirect it.
    silentlyCloseStandardStreams()

    val port = findRdPort(args)


    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(port) {
        synchronizationModel.initRemoteLogging.adviseOnce(lifetime) {
            Logger.set(Lifetime.Eternal, UtRdRemoteLoggerFactory(loggerModel))
            logger.info { "-----------------------------------------------------------------------" }
            logger.info { "------------------NEW SPRING ANALYZER PROCESS STARTED------------------" }
            logger.info { "-----------------------------------------------------------------------" }
        }
        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol.settingsModel))
        springAnalyzerProcessModel.setup(it, protocol)
    }
}

private fun SpringAnalyzerProcessModel.setup(watchdog: IdleWatchdog, realProtocol: IProtocol) {
    watchdog.measureTimeForActiveCall(analyze, "Analyzing Spring Application") { params ->
        SpringAnalyzerResult(
            SpringApplicationAnalyzer(
                ApplicationData(
                    params.classpath.toList().map { File(it).toURI().toURL() }.toTypedArray(),
                    params.configuration,
                    params.propertyFilesPaths.toList(),
                    params.xmlConfigurationPaths.toList()
                )
            ).analyze().toTypedArray()
        )
    }
}
