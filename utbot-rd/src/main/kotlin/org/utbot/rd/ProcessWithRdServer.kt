package org.utbot.rd

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.serverPort
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import com.jetbrains.rd.util.trace
import kotlinx.coroutines.delay
import org.utbot.common.getPid
import org.utbot.common.silent
import org.utbot.rd.generated.synchronizationModel
import java.io.File
import java.nio.file.Files

/**
 * Process will be terminated if lifetime is not alive
 */
suspend fun Process.withRdServer(
    lifetime: Lifetime? = null,
    serverFactory: (Lifetime) -> Protocol
): ProcessWithRdServer {
    return ProcessWithRdServerImpl(toLifetimedProcess(lifetime)) {
        serverFactory(it)
    }
}

suspend fun LifetimedProcess.withRdServer(
    serverFactory: (Lifetime) -> Protocol
): ProcessWithRdServer {
    return ProcessWithRdServerImpl(this) {
        serverFactory(it)
    }
}

/**
 * Process will not be started if lifetime is not alive
 */
suspend fun startProcessWithRdServer(
    cmd: List<String>,
    lifetime: Lifetime? = null,
    serverFactory: (Lifetime) -> Protocol
): ProcessWithRdServer {
    lifetime?.throwIfNotAlive()

    val child = startLifetimedProcess(cmd, lifetime)

    return child.withRdServer {
        serverFactory(it)
    }
}

/**
 * Process will not be started if lifetime is not alive
 */
suspend fun startProcessWithRdServer(
    processFactory: (Int) -> Process,
    lifetime: Lifetime? = null,
    serverFactory: (Int, Lifetime) -> Protocol
): ProcessWithRdServer {
    lifetime?.throwIfNotAlive()

    val port = NetUtils.findFreePort(0)

    return processFactory(port).withRdServer(lifetime) {
        serverFactory(port, it)
    }
}

/**
 * Main goals of this class:
 * 1. start rd server protocol with child process
 * 2. protocol should be bound to process lifetime
 */
interface ProcessWithRdServer : LifetimedProcess {
    val protocol: IProtocol
    val port: Int
        get() = protocol.wire.serverPort

    suspend fun awaitProcessReady(): ProcessWithRdServer
}

private val logger = getLogger<ProcessWithRdServer>()

class ProcessWithRdServerImpl private constructor(
    private val child: LifetimedProcess,
    serverFactory: (Lifetime) -> Protocol
) : ProcessWithRdServer, LifetimedProcess by child {
    override val protocol = serverFactory(lifetime)

    override fun terminate() {
        silent {
            protocol.synchronizationModel.stopProcess.fire(Unit)
        }
        child.terminate()
    }

    companion object {
        suspend operator fun invoke(
            child: LifetimedProcess, serverFactory: (Lifetime) -> Protocol
        ): ProcessWithRdServerImpl = ProcessWithRdServerImpl(child, serverFactory).terminateOnException {
            it.apply { protocol.wire.connected.adviseForConditionAsync(lifetime).await() }
        }
    }

    override suspend fun awaitProcessReady(): ProcessWithRdServer {
        protocol.scheduler.pump(lifetime) {
            protocol.synchronizationModel
        }
        processSyncDirectory.mkdirs()

        // there 2 stages at rd protocol initialization:
        // 1. we need to bind all entities - for ex. generated model and custom signal
        //  because we cannot operate with unbound
        // 2. we need to wait when all that entities bound on the other side
        //  because when we fire something that is not bound on another side - we will lose this call
        // to guarantee 2nd stage success - there is custom simple synchronization:
        // 1. child process will create file "${port}.created" - this indicates that child process is ready to receive messages
        // 2. we will test the connection via sync RdSignal
        // only then we can successfully start operating
        val pid = process.getPid.toInt()
        val syncFile = File(processSyncDirectory, childCreatedFileName(port))

        while (lifetime.isAlive) {
            if (Files.deleteIfExists(syncFile.toPath())) {
                logger.trace { "process $pid for port $port: signal file deleted connecting" }
                break
            }

            delay(fileWaitTimeoutMillis)
        }

        protocol.synchronizationModel.synchronizationSignal.let { sync ->
            val messageFromChild = sync.adviseForConditionAsync(lifetime) { it == "child" }

            while (messageFromChild.isActive) {
                sync.fire("main")
                delay(10)
            }
        }

        lifetime.onTermination {
            if (syncFile.exists()) {
                logger.trace { "process $pid for port $port: on terminating syncFile existed" }
                syncFile.delete()
            }
        }

        return this
    }
}