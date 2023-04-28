package org.utbot.instrumentation.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KotlinLogging
import org.utbot.common.currentProcessPid
import org.utbot.common.debug
import org.utbot.common.firstOrNullResourceIS
import org.utbot.common.getPid
import org.utbot.common.measureTime
import org.utbot.common.nameOfPackage
import org.utbot.common.scanForResourcesContaining
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.AbstractRDProcessCompanion
import org.utbot.instrumentation.agent.DynamicClassTransformer
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.DISABLE_SANDBOX_OPTION
import org.utbot.instrumentation.process.generated.AddPathsParams
import org.utbot.instrumentation.process.generated.InstrumentedProcessModel
import org.utbot.instrumentation.process.generated.SetInstrumentationParams
import org.utbot.instrumentation.process.generated.instrumentedProcessModel
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.loggers.UtRdKLogger
import org.utbot.rd.loggers.setup
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException
import java.io.File

private val logger = KotlinLogging.logger { }

private const val UTBOT_INSTRUMENTATION = "utbot-instrumentation-shadow"
private const val INSTRUMENTATION_LIB = "lib"

private fun tryFindInstrumentationJarInResources(): File? {
    logger.debug("Trying to find jar in the resources.")
    val tempDir = utBotTempDirectory.toFile()
    val unzippedJarName = "$UTBOT_INSTRUMENTATION-${currentProcessPid}.jar"
    val instrumentationJarFile = File(tempDir, unzippedJarName)

    InstrumentedProcess::class.java.classLoader
        .firstOrNullResourceIS(INSTRUMENTATION_LIB) { resourcePath ->
            resourcePath.contains(UTBOT_INSTRUMENTATION) && resourcePath.endsWith(".jar")
        }
        ?.use { input ->
            instrumentationJarFile.writeBytes(input.readBytes())
        } ?: return null
    return instrumentationJarFile
}

private fun tryFindInstrumentationJarOnClasspath(): File? {
    logger.debug("Trying to find it in the classpath.")
    return InstrumentedProcess::class.java.classLoader
        .scanForResourcesContaining(DynamicClassTransformer::class.java.nameOfPackage)
        .firstOrNull {
            it.absolutePath.contains(UTBOT_INSTRUMENTATION) && it.extension == "jar"
        }
}

private val instrumentationJarFile: File =
    logger.debug().measureTime({ "Finding $UTBOT_INSTRUMENTATION jar" } ) {
        tryFindInstrumentationJarInResources() ?: run {
            logger.debug("Failed to find jar in the resources.")
            tryFindInstrumentationJarOnClasspath()
        } ?: error("""
                    Can't find file: $UTBOT_INSTRUMENTATION.jar.
                    Make sure you added $UTBOT_INSTRUMENTATION.jar to the resources folder from gradle.
                """.trimIndent())
    }

class InstrumentedProcessInstantDeathException :
    InstantProcessDeathException(UtSettings.instrumentedProcessDebugPort, UtSettings.runInstrumentedProcessWithDebug)

/**
 * Main goals of this class:
 * 1. prepare started instrumented process for execution - initializing rd, sending paths and instrumentation
 * 2. expose bound model
 */
class InstrumentedProcess private constructor(
    private val classLoader: ClassLoader?,
    private val rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {
    val kryoHelper = KryoHelper(lifetime.createNested()).apply {
        classLoader?.let { setKryoClassLoader(it) }
    }
    val instrumentedProcessModel: InstrumentedProcessModel = onSchedulerBlocking { protocol.instrumentedProcessModel }
    val loggerModel: LoggerModel = onSchedulerBlocking { protocol.loggerModel }

    companion object : AbstractRDProcessCompanion(
        debugPort = UtSettings.instrumentedProcessDebugPort,
        runWithDebug = UtSettings.runInstrumentedProcessWithDebug,
        suspendExecutionInDebugMode = UtSettings.suspendInstrumentedProcessExecutionInDebugMode,
        processSpecificCommandLineArgs = {
            buildList {
                add("-javaagent:${instrumentationJarFile.path}")
                add("-ea")
                add("-jar")
                add(instrumentationJarFile.path)
                if (!UtSettings.useSandbox)
                    add(DISABLE_SANDBOX_OPTION)
            }
        }) {

        suspend operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            parent: Lifetime,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            classLoader: ClassLoader?
        ): InstrumentedProcess = parent.createNested().terminateOnException { lifetime ->
            val rdProcess: ProcessWithRdServer = startUtProcessWithRdServer(
                lifetime = lifetime
            ) { port ->
                val cmd = obtainProcessCommandLine(port)
                logger.debug { "Starting instrumented process: $cmd" }
                val directory = WorkingDirService.provide().toFile()
                val processBuilder = ProcessBuilder(cmd)
                    .directory(directory)
                val process = processBuilder.start()
                logger.info {
                        "------------------------------------------------------------------\n" +
                        "--------Instrumented process started with PID=${process.getPid}--------\n" +
                        "------------------------------------------------------------------"
                }
                if (!process.isAlive) {
                    throw InstrumentedProcessInstantDeathException()
                }
                process
            }.awaitProcessReady()

            logger.trace("rd process started")

            val proc = InstrumentedProcess(classLoader, rdProcess)
            proc.loggerModel.setup(logger, proc.lifetime)

            proc.lifetime.onTermination {
                logger.trace { "process is terminating" }
            }

            logger.trace("sending add paths")
            proc.instrumentedProcessModel.addPaths.startSuspending(
                proc.lifetime, AddPathsParams(
                    pathsToUserClasses,
                )
            )

            logger.trace("sending instrumentation")
            proc.instrumentedProcessModel.setInstrumentation.startSuspending(
                proc.lifetime, SetInstrumentationParams(
                    proc.kryoHelper.writeObject(instrumentation)
                )
            )
            logger.trace("start commands sent")

            return proc
        }
    }
}