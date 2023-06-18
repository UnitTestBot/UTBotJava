package org.utbot.spring.process

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.common.JarUtils
import org.utbot.common.getPid
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.services.WorkingDirService
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

class SpringAnalyzerProcessInstantDeathException :
    InstantProcessDeathException(
        UtSettings.springAnalyzerProcessDebugPort,
        UtSettings.runSpringAnalyzerProcessWithDebug
    )

private val logger = KotlinLogging.logger {}

private var classpathArgs = listOf<String>()

private const val SPRING_ANALYZER_JAR_FILENAME = "utbot-spring-analyzer-shadow.jar"

private val springAnalyzerJarFile =
    JarUtils.extractJarFileFromResources(
        jarFileName = SPRING_ANALYZER_JAR_FILENAME,
        jarResourcePath = "lib/$SPRING_ANALYZER_JAR_FILENAME",
        targetDirectoryName = "spring-analyzer"
    )

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
                        extendedClasspath.joinToString(File.pathSeparator),
                        "org.utbot.spring.process.SpringAnalyzerProcessMainKt"
                    )
                    val cmd = obtainProcessCommandLine(port)
                    val process = ProcessBuilder(cmd)
                        .directory(WorkingDirService.provide().toFile())
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
