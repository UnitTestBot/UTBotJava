package org.utbot.rd

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.serverPort
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.throwIfNotAlive

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
    val protocol: Protocol
    val port: Int
        get() = protocol.wire.serverPort
}

class ProcessWithRdServerImpl private constructor(
    private val child: LifetimedProcess,
    serverFactory: (Lifetime) -> Protocol
) : ProcessWithRdServer, LifetimedProcess by child {
    override val protocol = serverFactory(lifetime)

    companion object {
        suspend operator fun invoke(
            child: LifetimedProcess, serverFactory: (Lifetime) -> Protocol
        ): ProcessWithRdServerImpl = ProcessWithRdServerImpl(child, serverFactory).terminateOnException {
            it.apply { protocol.wire.connected.adviseForConditionAsync(lifetime).await() }
        }
    }
}