package org.utbot.framework.process

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.utbot.common.getPid
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.UtRdKLogger
import org.utbot.rd.loggers.setupRdLogger
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.rdPortArgument
import org.utbot.rd.startBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException
import org.utbot.spring.process.generated.SpringAnalyzerParams
import org.utbot.spring.process.generated.SpringAnalyzerProcessModel
import org.utbot.spring.process.generated.springAnalyzerProcessModel
import java.nio.file.Files

class SpringAnalyzerProcessInstantDeathException :
    InstantProcessDeathException(UtSettings.springAnalyzerProcessDebugPort, UtSettings.runSpringAnalyzerProcessWithDebug)

private const val SPRING_ANALYZER_JAR_FILENAME = "utbot-spring-analyzer-shadow.jar"
private val logger = KotlinLogging.logger {}
private val rdLogger = UtRdKLogger(logger, "")

class SpringAnalyzerProcess private constructor(
    rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {

    companion object {
        private fun obtainProcessSpecificCommandLineArgs(port: Int): List<String> {
            val jarFile =
                Files.createDirectories(utBotTempDirectory.toFile().resolve("spring-analyzer").toPath())
                    .toFile().resolve(SPRING_ANALYZER_JAR_FILENAME)
            FileUtils.copyURLToFile(
                this::class.java.classLoader.getResource("lib/$SPRING_ANALYZER_JAR_FILENAME"),
                jarFile
            )
            return listOf(
                "-Dorg.apache.commons.logging.LogFactory=org.utbot.rd.loggers.RDApacheCommonsLogFactory",
                "-jar",
                jarFile.path,
                rdPortArgument(port)
            )
        }

        fun createBlocking() = runBlocking { SpringAnalyzerProcess() }

        suspend operator fun invoke(): SpringAnalyzerProcess = LifetimeDefinition().terminateOnException { lifetime ->
            val rdProcess = startUtProcessWithRdServer(lifetime) { port ->
                val cmd = obtainCommonProcessCommandLineArgs(
                    debugPort = UtSettings.springAnalyzerProcessDebugPort,
                    runWithDebug = UtSettings.runSpringAnalyzerProcessWithDebug,
                    suspendExecutionInDebugMode = UtSettings.suspendSpringAnalyzerProcessExecutionInDebugMode,
                ) + obtainProcessSpecificCommandLineArgs(port)
                val process = ProcessBuilder(cmd)
                    .directory(Files.createTempDirectory(utBotTempDirectory, "spring-analyzer").toFile())
                    .start()

                logger.info { "Spring Analyzer process started with PID = ${process.getPid}" }

                if (!process.isAlive) throw SpringAnalyzerProcessInstantDeathException()

                process
            }
            rdProcess.awaitProcessReady()
            val proc = SpringAnalyzerProcess(rdProcess)
            setupRdLogger(rdProcess, proc.loggerModel, rdLogger)
            return proc
        }
    }

    private val springAnalyzerModel: SpringAnalyzerProcessModel = onSchedulerBlocking { protocol.springAnalyzerProcessModel }
    private val loggerModel: LoggerModel = onSchedulerBlocking { protocol.loggerModel }

    init {
        lifetime.onTermination {
            protocol.synchronizationModel.stopProcess.fire(Unit)
        }
    }

    fun getBeanQualifiedNames(
        classpath: List<String>,
        configuration: String,
        propertyFilesPaths: List<String>,
        xmlConfigurationPaths: List<String>
    ): List<String> {
        val params = SpringAnalyzerParams(
            classpath.toTypedArray(),
            configuration,
            propertyFilesPaths.toTypedArray(),
            xmlConfigurationPaths.toTypedArray()
        )
        val result = springAnalyzerModel.analyze.startBlocking(params)
        return result.beanTypes.toList()
    }
}
