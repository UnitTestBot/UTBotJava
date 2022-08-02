package org.utbot.rd

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.serverPort
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope

// process will be terminated if parent is not alive
suspend fun Process.withRdServer(
    parent: Lifetime? = null,
    serverFactory: (Lifetime) -> Protocol
): ProcessWithRdServer {
    return ProcessWithRdServerImpl(toLifetimedProcess(parent)) {
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

// process will not be started if parent is not alive
suspend fun startProcessWithRdServer(
    cmd: List<String>,
    parent: Lifetime? = null,
    serverFactory: (Lifetime) -> Protocol
): ProcessWithRdServer {
    parent?.throwIfNotAlive()

    val child = startLifetimedProcess(cmd, parent)

    return ProcessWithRdServerImpl(child, serverFactory)
}

// process will not be started if parent is not alive
suspend fun startProcessWithRdServer(
    processFactory: (Int) -> Process,
    parent: Lifetime? = null,
    serverFactory: (Int, Lifetime) -> Protocol
): ProcessWithRdServer {
    parent?.throwIfNotAlive()

    val port = NetUtils.findFreePort(0)

    return processFactory(port).withRdServer(parent) {
        serverFactory(port, it)
    }
}

// process will not be started if parent is not alive
suspend fun startProcessWithRdServer2(
    cmd: (Int) -> List<String>,
    parent: Lifetime? = null,
    serverFactory: (Int, Lifetime) -> Protocol
): ProcessWithRdServer {
    return startProcessWithRdServer({ ProcessBuilder(cmd(it)).start() }, parent, serverFactory)
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
    override val protocol = serverFactory(lifetime.createNested())

    companion object {
        suspend operator fun invoke(
            child: LifetimedProcess, serverFactory: (Lifetime) -> Protocol
        ): ProcessWithRdServerImpl = coroutineScope {
            ProcessWithRdServerImpl(child, serverFactory).apply {
                lifetime.usingNested { operation ->
                    val def = CompletableDeferred<Boolean>()

                    protocol.wire.connected.advise(operation) { isConnected ->
                        if (isConnected) {
                            def.complete(true)
                        }
                    }

                    operation.onTermination { def.cancel() }
                    def.await()
                }
            }
        }
    }
}