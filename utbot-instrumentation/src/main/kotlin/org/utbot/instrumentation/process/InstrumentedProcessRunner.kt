package org.utbot.instrumentation.process

import mu.KotlinLogging
import org.utbot.common.*
import org.utbot.common.scanForResourcesContaining
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.obtainCommonProcessCommandLineArgs
import org.utbot.instrumentation.agent.DynamicClassTransformer
import org.utbot.rd.rdPortArgument
import java.io.File

private val logger = KotlinLogging.logger {}

class InstrumentedProcessRunner {
    private val cmds: List<String> by lazy {
        obtainCommonProcessCommandLineArgs(
            debugPort = UtSettings.instrumentedProcessDebugPort,
            runWithDebug = UtSettings.runInstrumentedProcessWithDebug,
            suspendExecutionInDebugMode = UtSettings.suspendInstrumentedProcessExecutionInDebugMode
        ) + listOf("-javaagent:$jarFile", "-ea", "-jar", "$jarFile")
    }

    fun start(rdPort: Int): Process {
        val portArgument = rdPortArgument(rdPort)

        logger.debug { "Starting instrumented process: ${cmds.joinToString(" ")} $portArgument" }

        val directory = WorkingDirService.provide().toFile()
        val commandsWithOptions = buildList {
            addAll(cmds)
            if (!UtSettings.useSandbox) {
                add(DISABLE_SANDBOX_OPTION)
            }
            add(portArgument)
        }

        val processBuilder = ProcessBuilder(commandsWithOptions)
            .directory(directory)

        return processBuilder.start().also {
            logger.info {
                "------------------------------------------------------------------\n" +
                "--------Instrumented process started with PID=${it.getPid}--------\n" +
                "------------------------------------------------------------------"
            }
        }
    }

    companion object {
        private const val UTBOT_INSTRUMENTATION = "utbot-instrumentation"
        private const val INSTRUMENTATION_LIB = "lib"

        private val NULL_FILE_PATH: String = if (System.getProperty("os.name").startsWith("Windows")) {
            "NUL"
        } else {
            "/dev/null"
        }

        private val NULL_FILE = File(NULL_FILE_PATH)

        /**
         * * Firstly, searches for utbot-instrumentation jar in the classpath.
         *
         * * In case of failure, searches for utbot-instrumentation jar in the resources and extracts it to the
         * temporary directory. This jar file must be placed to the resources by `processResources` gradle task
         * in the gradle configuration of the project which depends on utbot-instrumentation module.
         */
        private val jarFile: File by lazy {
            logger.debug().measureTime({ "Finding $UTBOT_INSTRUMENTATION jar" } ) {
                run {
                    logger.debug("Trying to find jar in the resources.")
                    val tempDir = utBotTempDirectory.toFile()
                    val unzippedJarName = "$UTBOT_INSTRUMENTATION-${currentProcessPid}.jar"
                    val instrumentationJarFile = File(tempDir, unzippedJarName)

                    InstrumentedProcessRunner::class.java.classLoader
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
                    InstrumentedProcessRunner::class.java.classLoader
                        .scanForResourcesContaining(DynamicClassTransformer::class.java.nameOfPackage)
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