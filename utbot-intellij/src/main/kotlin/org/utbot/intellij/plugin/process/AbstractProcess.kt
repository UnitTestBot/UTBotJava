package org.utbot.intellij.plugin.process

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KLogger
import org.utbot.common.getPid
import org.utbot.common.osSpecificJavaExecutable
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.process.OpenModulesContainer
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.overrideDefaultRdLoggerFactoryWithKLogger
import org.utbot.rd.rdPortArgument
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.pathString

abstract class AbstractProcess protected constructor(
    private val rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {

    init {
        rdProcess.lifetime.onTermination {
            protocol.synchronizationModel.stopProcess.fire(Unit)
        }
    }

    /**
     * @param T additional data needed to instantiate the process
     * @param P process type
     */
    abstract class Companion<T, P : AbstractProcess>(
        private val displayName: String,
        private val logConfigFileGetter: () -> String,
        private val debugPortGetter: () -> Int,
        private val runWithDebugGetter: () -> Boolean,
        private val suspendExecutionInDebugModeGetter: () -> Boolean,
        private val logConfigurationsDirectory: File,
        private val logDirectory: File,
        logConfigurationFileDeleteKey: String,
        private val logAppender: String,
        private val currentLogFilename: String,
        protected val logger: KLogger,
    ) {
        private val deleteOpenComment = "<!--$logConfigurationFileDeleteKey"
        private val deleteCloseComment = "$logConfigurationFileDeleteKey-->"

        private val log4j2ConfigFile: File = run {
            logDirectory.mkdirs()
            logConfigurationsDirectory.mkdirs()
            overrideDefaultRdLoggerFactoryWithKLogger(logger)

            val customFile = File(logConfigFileGetter())

            if (customFile.exists()) customFile
            else {
                val log4j2ConfigFile = Files.createTempFile(logConfigurationsDirectory.toPath(), null, ".xml").toFile()
                this.javaClass.classLoader.getResourceAsStream("log4j2.xml")?.use { logConfig ->
                    val resultConfig = logConfig.readBytes().toString(Charset.defaultCharset())
                        .replace(Regex("$deleteOpenComment|$deleteCloseComment"), "")
                        .replace("ref=\"IdeaAppender\"", "ref=\"$logAppender\"")
                        .replace("\${env:UTBOT_LOG_DIR}", logDirectory.canonicalPath.trimEnd(File.separatorChar) + File.separatorChar)
                    Files.copy(
                        resultConfig.byteInputStream(),
                        log4j2ConfigFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
                log4j2ConfigFile
            }
        }

        private val log4j2ConfigSwitch = "-Dlog4j2.configurationFile=${log4j2ConfigFile.canonicalPath}"

        private val javaExecutablePathString: Path
            get() = JdkInfoService.jdkInfoProvider.info.path.resolve("bin${File.separatorChar}${osSpecificJavaExecutable()}")

        private fun suspendValue(): String = if (suspendExecutionInDebugModeGetter()) "y" else "n"

        private val debugArgument: String?
            get() = "-agentlib:jdwp=transport=dt_socket,server=n,suspend=${suspendValue()},quiet=y,address=${debugPortGetter()}"
                .takeIf { runWithDebugGetter() }

        protected abstract fun obtainProcessSpecificCommandLineArgs(): List<String>

        private fun obtainProcessCommandLineArgs(port: Int) = buildList {
            add(javaExecutablePathString.pathString)
            val javaVersionSpecificArgs = OpenModulesContainer.javaVersionSpecificArguments
            if (javaVersionSpecificArgs.isNotEmpty()) {
                addAll(javaVersionSpecificArgs)
            }
            debugArgument?.let { add(it) }
            add(log4j2ConfigSwitch)
            addAll(obtainProcessSpecificCommandLineArgs())
            add(rdPortArgument(port))
        }

        protected abstract fun createFromRDProcess(params: T, rdProcess: ProcessWithRdServer) : P

        protected abstract fun getWorkingDirectory(): File

        protected abstract fun createInstantDeathException(): InstantProcessDeathException

        fun createBlocking(params: T): P = runBlocking { invoke(params) }

        suspend operator fun invoke(params: T): P =
            LifetimeDefinition().terminateOnException { lifetime ->
                val rdProcess = startUtProcessWithRdServer(lifetime) { port ->
                    val cmd = obtainProcessCommandLineArgs(port)
                    val process = ProcessBuilder(cmd)
                        .directory(getWorkingDirectory())
                        .start()

                    logger.info { "$displayName process started with PID = ${process.getPid}" }
                    logger.info { "$displayName log directory - ${logDirectory.canonicalPath}" }
                    logger.info { "$displayName log file - ${logDirectory.resolve(currentLogFilename)}" }
                    logger.info { "Log4j2 configuration file path - ${log4j2ConfigFile.canonicalPath}" }

                    if (!process.isAlive) throw createInstantDeathException()

                    process
                }
                rdProcess.awaitProcessReady()

                return createFromRDProcess(params, rdProcess)
            }
    }
}
