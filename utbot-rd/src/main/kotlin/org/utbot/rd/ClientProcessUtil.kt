package org.utbot.rd

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.trace
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.utbot.common.*
import org.utbot.rd.generated.synchronizationModel
import java.io.File
import kotlin.time.Duration

const val rdProcessDirName = "rdProcessSync"
val processSyncDirectory = File(utBotTempDirectory.toFile(), rdProcessDirName)
const val rdPortProcessArgumentTag = "rdPort"
internal const val fileWaitTimeoutMillis = 10L
private val logger = getLogger<ClientProtocolBuilder>()

internal fun childCreatedFileName(port: Int): String {
    return "$port.created"
}

internal fun signalChildReady(port: Int) {
    processSyncDirectory.mkdirs()

    val signalFile = File(processSyncDirectory, childCreatedFileName(port))

    if (signalFile.exists()) {
        signalFile.delete()
    }

    val created = signalFile.createNewFile()

    if (!created) {
        throw IllegalStateException("cannot create signal file")
    }
}

fun rdPortArgument(port: Int): String {
    return "$rdPortProcessArgumentTag=$port"
}

fun findRdPort(args: Array<String>): Int {
    return args.find { it.startsWith(rdPortProcessArgumentTag) }
        ?.run { split("=").last().toInt().coerceIn(1..65535) }
        ?: throw IllegalArgumentException("No port provided")
}

class CallsSynchronizer(private val ldef: LifetimeDefinition, val timeout: Duration) {
    private enum class State {
        STARTED,
        ENDED
    }

    private val synchronizer: Channel<State> = Channel(1)

    fun <T> measureExecutionForTermination(block: () -> T): T = runBlocking {
        try {
            synchronizer.send(State.STARTED)
            return@runBlocking block()
        } finally {
            synchronizer.send(State.ENDED)
        }
    }

    fun <T, R> measureExecutionForTermination(call: RdCall<T, R>, block: (T) -> R) {
        call.set { it ->
            runBlocking {
                measureExecutionForTermination {
                    block(it)
                }
            }
        }
    }

    suspend fun setupTimeout() {
        ldef.launch {
            var lastState = State.ENDED
            while (ldef.isAlive) {
                val current: State? =
                    withTimeoutOrNull(timeout) {
                        synchronizer.receive()
                    }
                if (current == null) {
                    if (lastState == State.ENDED) {
                        // process is waiting for command more than expected, better die
                        logger.info { "terminating lifetime by timeout" }
                        stopProtocol()
                        break
                    }
                } else {
                    lastState = current
                }
            }
        }
    }

    fun stopProtocol() {
        ldef.terminate()
    }
}

class ClientProtocolBuilder {
    private var timeout = Duration.INFINITE

    suspend fun start(port: Int, parent: Lifetime? = null, bindables: Protocol.(CallsSynchronizer) -> Unit) {
        UtRdCoroutineScope.current // coroutine scope initialization
        val pid = currentProcessPid.toInt()
        val ldef = parent?.createNested() ?: LifetimeDefinition()
        ldef.terminateOnException { _ ->
            ldef += { logger.info { "lifetime terminated" } }
            ldef += {
                val syncFile = File(processSyncDirectory, childCreatedFileName(port))

                if (syncFile.exists()) {
                    logger.info { "sync file existed" }
                    syncFile.delete()
                }
            }
            logger.info { "pid - $pid, port - $port" }
            logger.info { "isJvm8 - $isJvm8, isJvm9Plus - $isJvm9Plus, isWindows - $isWindows" }

            val name = "Client$port"
            val rdClientProtocolScheduler = SingleThreadScheduler(ldef, "Scheduler for $name")
            val clientProtocol = Protocol(
                name,
                Serializers(),
                Identities(IdKind.Client),
                rdClientProtocolScheduler,
                SocketWire.Client(ldef, rdClientProtocolScheduler, port),
                ldef
            )
            val synchronizer = CallsSynchronizer(ldef, timeout)

            synchronizer.setupTimeout()
            rdClientProtocolScheduler.pump(ldef) {
                clientProtocol.synchronizationModel
                clientProtocol.bindables(synchronizer)
            }

            signalChildReady(port)
            logger.info { "signalled" }
            clientProtocol.synchronizationModel.synchronizationSignal.let { sync ->
                val answerFromMainProcess = sync.adviseForConditionAsync(ldef) {
                    if (it == "main") {
                        logger.trace { "received from main" }
                        synchronizer.measureExecutionForTermination {
                            sync.fire("child")
                        }
                        true
                    } else {
                        false
                    }
                }
                answerFromMainProcess.await()
            }
            ldef.awaitTermination()
        }
    }

    fun withProtocolTimeout(duration: Duration): ClientProtocolBuilder {
        timeout = duration
        return this
    }
}