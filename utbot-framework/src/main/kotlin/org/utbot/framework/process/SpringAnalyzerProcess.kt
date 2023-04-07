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
import org.utbot.rd.loggers.UtRdKLogger
import org.utbot.rd.loggers.setup
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException
import org.utbot.rd.generated.SpringAnalyzerParams
import org.utbot.rd.generated.SpringAnalyzerProcessModel
import org.utbot.rd.generated.springAnalyzerProcessModel
import java.nio.file.Files

class SpringAnalyzerProcessInstantDeathException :
    InstantProcessDeathException(
        UtSettings.springAnalyzerProcessDebugPort,
        UtSettings.runSpringAnalyzerProcessWithDebug
    )

private const val SPRING_ANALYZER_JAR_FILENAME = "utbot-spring-analyzer-shadow.jar"
private const val SPRING_ANALYZER_JAR_PATH = "lib/$SPRING_ANALYZER_JAR_FILENAME"
private const val UNKNOWN_MODIFICATION_TIME = 0L
private val logger = KotlinLogging.logger {}
private val rdLogger = UtRdKLogger(logger, "")

private val jarFile = Files.createDirectories(utBotTempDirectory.toFile().resolve("spring-analyzer").toPath())
    .toFile().resolve(SPRING_ANALYZER_JAR_FILENAME).also { jarFile ->
        val resource = SpringAnalyzerProcess::class.java.classLoader.getResource(SPRING_ANALYZER_JAR_PATH)
            ?: error("Unable to find \"$SPRING_ANALYZER_JAR_PATH\" in resources, make sure it's on the classpath")
        val resourceConnection = resource.openConnection()
        val lastResourceModification = try {
            resourceConnection.lastModified
        } finally {
            resourceConnection.getInputStream().close()
        }
        if (
            !jarFile.exists() ||
            jarFile.lastModified() == UNKNOWN_MODIFICATION_TIME ||
            lastResourceModification == UNKNOWN_MODIFICATION_TIME ||
            jarFile.lastModified() < lastResourceModification
        )
            FileUtils.copyURLToFile(resource, jarFile)
    }

class SpringAnalyzerProcess private constructor(
    rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {

    companion object : AbstractRDProcessCompanion(
        debugPort = UtSettings.springAnalyzerProcessDebugPort,
        runWithDebug = UtSettings.runSpringAnalyzerProcessWithDebug,
        suspendExecutionInDebugMode = UtSettings.suspendSpringAnalyzerProcessExecutionInDebugMode,
        processSpecificCommandLineArgs = listOf(
            "-Dorg.apache.commons.logging.LogFactory=org.utbot.spring.loggers.RDApacheCommonsLogFactory",
            "-jar",
            jarFile.path
        )
    ) {
        fun createBlocking() = runBlocking { SpringAnalyzerProcess() }

        suspend operator fun invoke(): SpringAnalyzerProcess = LifetimeDefinition().terminateOnException { lifetime ->
            val rdProcess = startUtProcessWithRdServer(lifetime) { port ->
                val cmd = obtainProcessCommandLine(port)
                val process = ProcessBuilder(cmd)
                    .directory(Files.createTempDirectory(utBotTempDirectory, "spring-analyzer").toFile())
                    .start()

                logger.info { "Spring Analyzer process started with PID = ${process.getPid}" }

                if (!process.isAlive) throw SpringAnalyzerProcessInstantDeathException()

                process
            }
            rdProcess.awaitProcessReady()
            val proc = SpringAnalyzerProcess(rdProcess)
            proc.loggerModel.setup(rdLogger, proc.lifetime)
            return proc
        }
    }

    private val springAnalyzerModel: SpringAnalyzerProcessModel = onSchedulerBlocking { protocol.springAnalyzerProcessModel }
    private val loggerModel: LoggerModel = onSchedulerBlocking { protocol.loggerModel }

    fun getBeanQualifiedNames(
        classpath: List<String>,
        configuration: String,
        propertyFilesPaths: List<String>,
        xmlConfigurationPaths: List<String>,
        useSpringAnalyzer: Boolean
    ): List<String> {
        val params = SpringAnalyzerParams(
            classpath.toTypedArray(),
            configuration,
            propertyFilesPaths.toTypedArray(),
            xmlConfigurationPaths.toTypedArray(),
            useSpringAnalyzer
        )
        val result = springAnalyzerModel.analyze.startBlocking(params)
        return result.beanTypes.toList()
    }
}
