package org.utbot.instrumentation.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import mu.KotlinLogging
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.rd.generated.AddPathsParams
import org.utbot.instrumentation.rd.generated.ChildProcessModel
import org.utbot.instrumentation.rd.generated.SetInstrumentationParams
import org.utbot.instrumentation.rd.generated.childProcessModel
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.rd.terminateOnException

private val logger = KotlinLogging.logger {}

/**
 * Main goals of this class:
 * 1. prepare started child process for execution - initializing rd, sending paths and instrumentation
 * 2. expose bound model
 */
class UtInstrumentationProcess private constructor(
    private val classLoader: ClassLoader?,
    private val rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {
    val kryoHelper = KryoHelper(lifetime.createNested()).apply {
        classLoader?.let { setKryoClassLoader(it) }
    }
    val chidlProcessModel: ChildProcessModel = onSchedulerBlocking { protocol.childProcessModel }

    companion object {
        suspend operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            parent: Lifetime,
            childProcessRunner: ChildProcessRunner,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String,
            classLoader: ClassLoader?
        ): UtInstrumentationProcess = parent.createNested().terminateOnException { lifetime ->
            val rdProcess: ProcessWithRdServer = startUtProcessWithRdServer(
                lifetime = lifetime
            ) {
                childProcessRunner.start(it)
            }.awaitSignal()

            logger.trace("rd process started")

            val proc = UtInstrumentationProcess(
                classLoader,
                rdProcess
            )

            proc.lifetime.onTermination {
                logger.trace { "process is terminating" }
            }

            logger.trace("sending add paths")
            proc.chidlProcessModel.addPaths.startSuspending(
                proc.lifetime, AddPathsParams(
                    pathsToUserClasses,
                    pathsToDependencyClasses
                )
            )

            logger.trace("sending instrumentation")
            proc.chidlProcessModel.setInstrumentation.startSuspending(
                proc.lifetime, SetInstrumentationParams(
                    proc.kryoHelper.writeObject(instrumentation)
                )
            )
            logger.trace("start commands sent")

            return proc
        }
    }
}