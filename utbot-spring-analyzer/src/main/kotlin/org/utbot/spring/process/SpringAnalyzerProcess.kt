package org.utbot.spring.process

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.utbot.common.getPid
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.process.AbstractRDProcessCompanion
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.loggers.setup
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException
import org.utbot.spring.generated.SpringAnalyzerParams
import org.utbot.spring.generated.SpringAnalyzerProcessModel
import org.utbot.spring.generated.SpringAnalyzerResult
import org.utbot.spring.generated.springAnalyzerProcessModel
import java.io.File
import java.net.URL
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

private var classpathArgs = listOf<String>()

private val springAnalyzerDirectory =
    Files.createDirectories(utBotTempDirectory.toFile().resolve("spring-analyzer").toPath()).toFile()

private val springAnalyzerJarFile =
    springAnalyzerDirectory
        .resolve(SPRING_ANALYZER_JAR_FILENAME).also { jarFile ->
            val resource = SpringAnalyzerProcess::class.java.classLoader.getResource(SPRING_ANALYZER_JAR_PATH)
                    ?: error("Unable to find \"$SPRING_ANALYZER_JAR_PATH\" in resources, make sure it's on the classpath")
            updateJarIfRequired(jarFile, resource)
        }

private fun updateJarIfRequired(jarFile: File, resource: URL) {
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
    ) {
        FileUtils.copyURLToFile(resource, jarFile)
    }
}

class SpringAnalyzerProcess private constructor(
    rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {

    companion object : AbstractRDProcessCompanion(
        debugPort = UtSettings.springAnalyzerProcessDebugPort,
        runWithDebug = UtSettings.runSpringAnalyzerProcessWithDebug,
        suspendExecutionInDebugMode = UtSettings.suspendSpringAnalyzerProcessExecutionInDebugMode,
        processSpecificCommandLineArgs = {
            listOf("-Dorg.apache.commons.logging.LogFactory=org.utbot.spring.loggers.RDApacheCommonsLogFactory") + classpathArgs
        }
    ) {
        fun createBlocking(classpath: List<String>) = runBlocking { SpringAnalyzerProcess(classpath) }

        suspend operator fun invoke(classpathItems: List<String>): SpringAnalyzerProcess =
            LifetimeDefinition().terminateOnException { lifetime ->
                val extendedClasspath = listOf(springAnalyzerJarFile.path) + classpathItems

                val rdProcess = startUtProcessWithRdServer(lifetime) { port ->
                    classpathArgs = listOf(
                        "-cp",
                        "\"${extendedClasspath.joinToString(File.pathSeparator)}\"",
                        "org.utbot.spring.process.SpringAnalyzerProcessMainKt"
                    )
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
                proc.loggerModel.setup(logger, proc.lifetime)
                return proc
            }
    }

    private val springAnalyzerModel: SpringAnalyzerProcessModel = onSchedulerBlocking { protocol.springAnalyzerProcessModel }
    private val loggerModel: LoggerModel = onSchedulerBlocking { protocol.loggerModel }

    fun getBeanDefinitions(
        configuration: String,
        fileStorage: Array<String>,
        profileExpression: String?,
    ): SpringAnalyzerResult {
        val params = SpringAnalyzerParams(configuration, fileStorage, profileExpression)
        return springAnalyzerModel.analyze.startBlocking(params)
    }
}
