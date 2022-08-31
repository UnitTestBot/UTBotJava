package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.utbot.common.getPid
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.rd.generated.AddPathsParams
import org.utbot.instrumentation.rd.generated.ProtocolModel
import org.utbot.instrumentation.rd.generated.SetInstrumentationParams
import org.utbot.instrumentation.rd.generated.protocolModel
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}
private const val fileWaitTimeoutMillis = 10L

/**
 * Main goals of this class:
 * 1. prepare started child process for execution - initializing rd, sending paths and instrumentation
 * 2. expose bound model
 */
class UtInstrumentationProcess private constructor(
    private val classLoader: ClassLoader?,
    private val rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {
    private val sync = RdSignal<String>().static(1).apply { async = true }
    val kryoHelper = KryoHelper(lifetime.createNested()).apply {
        classLoader?.let { setKryoClassLoader(it) }
    }
    val protocolModel: ProtocolModel
        get() = protocol.protocolModel

    private suspend fun init(): UtInstrumentationProcess {
        protocol.scheduler.pump(lifetime) {
            sync.bind(lifetime, protocol, sync.rdid.toString())
            protocol.protocolModel
        }
        processSyncDirectory.mkdirs()

        // there 2 stages at rd protocol initialization:
        // 1. we need to bind all entities - for ex. generated model and custom signal
        //  because we cannot operate with unbound
        // 2. we need to wait when all that entities bound on the other side
        //  because when we fire something that is not bound on another side - we will lose this call
        // to guarantee 2nd stage success - there is custom simple synchronization:
        // 1. child process will create file "${processId}.created" - this indicates that child process is ready to receive messages
        // 2. we will test the connection via sync RdSignal
        // only then we can successfully start operating
        val pid = process.getPid.toInt()
        val syncFile = File(processSyncDirectory, childCreatedFileName(pid))

        while (lifetime.isAlive) {
            if (Files.deleteIfExists(syncFile.toPath())) {
                logger.trace { "process $pid: signal file deleted connecting" }
                break
            }

            delay(fileWaitTimeoutMillis)
        }

        val messageFromChild = sync.adviseForConditionAsync(lifetime) { it == "child" }

        while(messageFromChild.isActive) {
            sync.fire("main")
            delay(10)
        }

        lifetime.onTermination {
            if (syncFile.exists()) {
                logger.trace { "process $pid: on terminating syncFile existed" }
                syncFile.delete()
            }
        }

        return this
    }

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
            }
            logger.trace("rd process started")
            val proc = UtInstrumentationProcess(
                classLoader,
                rdProcess
            ).init()

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