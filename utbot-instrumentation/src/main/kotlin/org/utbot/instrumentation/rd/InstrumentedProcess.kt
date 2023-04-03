package org.utbot.instrumentation.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KotlinLogging
import org.utbot.framework.UtSettings
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.InstrumentedProcessRunner
import org.utbot.instrumentation.process.generated.AddPathsParams
import org.utbot.instrumentation.process.generated.InstrumentedProcessModel
import org.utbot.instrumentation.process.generated.SetInstrumentationParams
import org.utbot.instrumentation.process.generated.instrumentedProcessModel
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.UtRdKLogger
import org.utbot.rd.loggers.UtRdRemoteLogger
import org.utbot.rd.loggers.setupRdLogger
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException

private val logger = KotlinLogging.logger { }
private val rdLogger = UtRdKLogger(logger, "")

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

    companion object {
        suspend operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            parent: Lifetime,
            instrumentedProcessRunner: InstrumentedProcessRunner,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            classLoader: ClassLoader?
        ): InstrumentedProcess = parent.createNested().terminateOnException { lifetime ->
            val rdProcess: ProcessWithRdServer = startUtProcessWithRdServer(
                lifetime = lifetime
            ) {
                val process = instrumentedProcessRunner.start(it)
                if (!process.isAlive) {
                    throw InstrumentedProcessInstantDeathException()
                }
                process
            }.awaitProcessReady()

            logger.trace("rd process started")

            val proc = InstrumentedProcess(classLoader, rdProcess)
            setupRdLogger(rdProcess, proc.loggerModel, rdLogger)

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