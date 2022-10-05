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
    val protocolModel: ChildProcessModel
        get() = protocol.childProcessModel

    companion object {
        private suspend fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invokeImpl(
            lifetime: Lifetime,
            childProcessRunner: ChildProcessRunner,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String,
            classLoader: ClassLoader?
        ): UtInstrumentationProcess {
            val rdProcess: ProcessWithRdServer = startUtProcessWithRdServer(
                lifetime = lifetime
            ) {
                childProcessRunner.start(it)
            }.initModels { childProcessModel }.awaitSignal()

            logger.trace("rd process started")

            val proc = UtInstrumentationProcess(
                classLoader,
                rdProcess
            )

            proc.lifetime.onTermination {
                logger.trace { "process is terminating" }
            }

            logger.trace("sending add paths")
            proc.protocolModel.addPaths.startSuspending(
                proc.lifetime, AddPathsParams(
                    pathsToUserClasses,
                    pathsToDependencyClasses
                )
            )

            logger.trace("sending instrumentation")
            proc.protocolModel.setInstrumentation.startSuspending(
                proc.lifetime, SetInstrumentationParams(
                    proc.kryoHelper.writeObject(instrumentation)
                )
            )
            logger.trace("start commands sent")

            return proc
        }

        suspend operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            lifetime: Lifetime,
            childProcessRunner: ChildProcessRunner,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String,
            classLoader: ClassLoader?
        ): UtInstrumentationProcess = lifetime.createNested().terminateOnException {
            invokeImpl(
                it,
                childProcessRunner,
                instrumentation,
                pathsToUserClasses,
                pathsToDependencyClasses,
                classLoader
            )
        }
    }
}