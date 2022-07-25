package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.serverPort
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import org.utbot.common.utBotTempDirectory
import java.io.File

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

suspend fun startProcessWithRdServer(
    cmd: List<String>,
    parent: Lifetime? = null,
    serverFactory: (Lifetime) -> Protocol
): ProcessWithRdServer {
    val child = startLifetimedProcess(cmd, parent)

    return ProcessWithRdServerImpl(child, serverFactory)
}

suspend fun startProcessWithRdServer(
    processFactory: (Int) -> Process,
    parent: Lifetime? = null,
    serverFactory: (Int, Lifetime) -> Protocol
): ProcessWithRdServer {
    val port = NetUtils.findFreePort(0)

    return processFactory(port).withRdServer(parent) {
        serverFactory(port, it)
    }
}

suspend fun startProcessWithRdServer2(
    cmd: (Int) -> List<String>,
    parent: Lifetime? = null,
    serverFactory: (Int, Lifetime) -> Protocol
): ProcessWithRdServer {
    return startProcessWithRdServer({ ProcessBuilder(cmd(it)).start() }, parent, serverFactory)
}

const val rdProcessDirName = "rdProcessSync"
val processSyncDirectory = File(utBotTempDirectory.toFile(), rdProcessDirName)

/**
 * Main goals of this class:
 * 1. start rd server protocol with child process
 * 2. protocol should be bound to process lifetime
 * 3. optionally wait until child process starts client protocol and connects
 *
 * To achieve step 3:
 * 1. child process should start client ASAP, preferably should be the first thing done when child starts
 * 2. serverFactory must create protocol with provided child process lifetime
 * 3. server and client protocol should choose same port,
 *      preferable way is to find open port in advance, provide it to child process via process arguments and
 *      have serverFactory use it
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