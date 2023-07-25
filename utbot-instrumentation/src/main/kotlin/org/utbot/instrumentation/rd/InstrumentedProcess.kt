package org.utbot.instrumentation.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KotlinLogging
import org.utbot.common.JarUtils
import org.utbot.common.debug
import org.utbot.common.getPid
import org.utbot.common.measureTime
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.AbstractRDProcessCompanion
import org.utbot.framework.process.kryo.KryoHelper
import org.utbot.instrumentation.agent.DynamicClassTransformer
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.DISABLE_SANDBOX_OPTION
import org.utbot.instrumentation.process.generated.AddPathsParams
import org.utbot.instrumentation.process.generated.GetSpringBeanParams
import org.utbot.instrumentation.process.generated.InstrumentedProcessModel
import org.utbot.instrumentation.process.generated.SetInstrumentationParams
import org.utbot.instrumentation.process.generated.instrumentedProcessModel
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.loggers.setup
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException
import java.io.File

private val logger = KotlinLogging.logger { }

private const val UTBOT_INSTRUMENTATION_JAR_FILENAME = "utbot-instrumentation-shadow.jar"

private val instrumentationJarFile: File =
    logger.debug().measureTime({ "Finding $UTBOT_INSTRUMENTATION_JAR_FILENAME jar" } ) {
        try {
            JarUtils.extractJarFileFromResources(
                jarFileName = UTBOT_INSTRUMENTATION_JAR_FILENAME,
                jarResourcePath = "lib/$UTBOT_INSTRUMENTATION_JAR_FILENAME",
                targetDirectoryName = "utbot-instrumentation"
            )
        } catch (e: Exception) {
            throw IllegalStateException(
                """
                    Can't find file: $UTBOT_INSTRUMENTATION_JAR_FILENAME.
                    Make sure you added $UTBOT_INSTRUMENTATION_JAR_FILENAME to the resources folder from gradle.
                """.trimIndent(),
                e
            )
        }
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
            instrumentationFactory: Instrumentation.Factory<TIResult, TInstrumentation>,
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
                    proc.kryoHelper.writeObject(instrumentationFactory)
                )
            )
            logger.trace("start commands sent")

            return proc
        }
    }

    fun getBean(beanName: String): UtModel =
        kryoHelper.readObject(instrumentedProcessModel.getSpringBean.startBlocking(GetSpringBeanParams(beanName)).beanModel)
}