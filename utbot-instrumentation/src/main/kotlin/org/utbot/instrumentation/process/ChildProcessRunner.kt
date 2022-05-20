package org.utbot.instrumentation.process

import org.utbot.common.bracket
import org.utbot.common.debug
import org.utbot.common.firstOrNullResourceIS
import org.utbot.common.getCurrentProcessId
import org.utbot.common.packageName
import org.utbot.common.pid
import org.utbot.common.scanForResourcesContaining
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.JdkPathService
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.agent.DynamicClassTransformer
import java.io.File
import java.nio.file.Paths
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private var processSeqN = 0

class ChildProcessRunner {
    private val cmds: List<String> by lazy {
        val debugCmd = if (Settings.runChildProcessWithDebug) {
            listOf(DEBUG_RUN_CMD)
        } else {
            emptyList()
        }

        listOf(Paths.get(JdkPathService.jdkPath.toString(),"bin", "java").toString()) +
                debugCmd + listOf("-javaagent:$jarFile", "-ea", "-jar", "$jarFile")
    }

    lateinit var errorLogFile: File

    fun start(): Process {
        logger.debug { "Starting child process: ${cmds.joinToString(" ")}" }
        processSeqN++

        val dir = File(utBotTempDirectory.toFile(), ERRORS_FILE_PREFIX)
        dir.mkdirs()
        errorLogFile = File(dir, "${hashCode()}-${processSeqN}.log")

        val processBuilder = ProcessBuilder(cmds).redirectError(errorLogFile)
        return processBuilder.start().also {
            logger.debug { "Process started with PID=${it.pid} , error log: ${errorLogFile.absolutePath}" }
        }
    }

    companion object {
        private const val UTBOT_INSTRUMENTATION = "utbot-instrumentation"
        private const val ERRORS_FILE_PREFIX = "utbot-childprocess-errors"
        private const val INSTRUMENTATION_LIB = "instrumentation-lib"

        private const val DEBUG_RUN_CMD = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005"

        /**
         * * Firstly, searches for utbot-instrumentation jar in the classpath.
         *
         * * In case of failure, searches for utbot-instrumentation jar in the resources and extracts it to the
         * temporary directory. This jar file must be placed to the resources by `processResources` gradle task
         * in the gradle configuration of the project which depends on utbot-instrumentation module.
         */
        private val jarFile: File by lazy {
            logger.debug().bracket("Finding $UTBOT_INSTRUMENTATION jar") {
                run {
                    logger.debug("Trying to find jar in the resources.")
                    val tempDir = utBotTempDirectory.toFile()
                    val unzippedJarName = "$UTBOT_INSTRUMENTATION-${getCurrentProcessId()}.jar"
                    val instrumentationJarFile = File(tempDir, unzippedJarName)

                    ChildProcessRunner::class.java.classLoader
                        .firstOrNullResourceIS(INSTRUMENTATION_LIB) { resourcePath ->
                            resourcePath.contains(UTBOT_INSTRUMENTATION) && resourcePath.endsWith(".jar")
                        }
                        ?.use { input ->
                            instrumentationJarFile.writeBytes(input.readBytes())
                        }
                        ?: return@run null
                    instrumentationJarFile
                } ?: run {
                    logger.debug("Failed to find jar in the resources. Trying to find it in the classpath.")
                    ChildProcessRunner::class.java.classLoader
                        .scanForResourcesContaining(DynamicClassTransformer::class.java.packageName)
                        .firstOrNull {
                            it.absolutePath.contains(UTBOT_INSTRUMENTATION) && it.extension == "jar"
                        }
                } ?: error("""
                    Can't find file: $UTBOT_INSTRUMENTATION-<version>.jar.
                    Make sure you added $UTBOT_INSTRUMENTATION-<version>.jar to the resources folder from gradle.
                """.trimIndent())
            }
        }
    }
}