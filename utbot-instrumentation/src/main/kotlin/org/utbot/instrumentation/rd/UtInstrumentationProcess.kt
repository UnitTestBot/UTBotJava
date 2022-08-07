package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.base.bind
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.utbot.common.catch
import org.utbot.common.pid
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.instrumentation.util.Protocol
import org.utbot.instrumentation.util.ReadingFromKryoException
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.UtRdCoroutineScope
import org.utbot.rd.UtRdUtil
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

val logger = KotlinLogging.logger{}
private const val fileWaitTimeoutMillis = 10L

/**
 * Main goals of this class:
 * 1. prepare started child process for execution - initializing rd, sending paths and instrumentation
 * 2. communicate with child process, i.e. send and receive messages
 *
 * facts
 * 1. process lifetime - helps indicate that child died
 * 2. operation lifetime - terminates either by process lifetime or coroutine scope cancellation, i.e. timeout
 * also removes orphaned continuation on termination
 * 3. child process operations are executed consequently and must always return exactly one answer command
 * 4. if process is dead - it always throws CancellationException on any operation
 * do not allow to obtain dead process, return newly restart instance it if terminated
 *
 * 3. optionally wait until child process starts client protocol and connects
 *
 * To achieve step 3:
 * 1. child process should start client ASAP, preferably should be the first thing done when child starts
 * 2. serverFactory must create protocol with provided child process lifetime
 * 3. server and client protocol should choose same port,
 *      preferable way is to find open port in advance, provide it to child process via process arguments and
 *      have serverFactory use it
 */
class UtInstrumentationProcess private constructor(
    private val classLoader: ClassLoader?,
    private val rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {
    private var lastSentId = 0L
    private var lastReceivedId = 0L
    private val callbacks: ConcurrentMap<Long, CompletableDeferred<Protocol.Command>> = ConcurrentSkipListMap()
    private val toProcess = RdSignal<ByteArray>().static(1).apply { async = true }
    private val fromProcess = RdSignal<ByteArray>().static(2).apply { async = true }
    private val sync = RdSignal<String>().static(3).apply { async = true }

    private suspend fun init(): UtInstrumentationProcess {
        lifetime.usingNested { operation ->
            val bound = CompletableDeferred<Boolean>()

            protocol.scheduler.invokeOrQueue {
                toProcess.bind(lifetime, protocol, toProcess.rdid.toString())
                fromProcess.bind(lifetime, protocol, fromProcess.rdid.toString())
                sync.bind(lifetime, protocol, sync.rdid.toString())
                bound.complete(true)
            }
            operation.onTermination { bound.cancel() }
            bound.await()
        }
        processSyncDirectory.mkdirs()

        val syncFile = File(processSyncDirectory, childCreatedFileName(process.pid.toInt()))

        while (lifetime.isAlive) {
            if (Files.deleteIfExists(syncFile.toPath())) {
                logger.trace { "process ${process.pid}: signal file deleted connecting" }
                break
            }

            delay(fileWaitTimeoutMillis)
        }

        lifetime.usingNested { syncLifetime ->
            val childReady = AtomicBoolean(false)
            sync.advise(syncLifetime) {
                if (it == "child") {
                    childReady.set(true)
                }
            }

            while (!childReady.get()) {
                sync.fire("main")
                delay(10)
            }
        }

        lifetime.onTermination {
            if (syncFile.exists()) {
                logger.trace{ "process ${process.pid}: on terminating syncFile existed" }
                syncFile.delete()
            }
        }

        kryoHelper = KryoHelper(
            rdProcess.lifetime.createNested(),
            fromProcess,
            toProcess,
            logger::trace
        ).apply {
            classLoader?.let { setKryoClassLoader(it) }
        }

        receiver.start()

        return this
    }

    private val receiver: Thread
    private lateinit var kryoHelper: KryoHelper

    companion object {
        suspend operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            parent: Lifetime,
            childProcessRunner: ChildProcessRunner,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String,
            classLoader: ClassLoader?
        ): UtInstrumentationProcess {
            val rdProcess: ProcessWithRdServer = UtRdUtil.startUtProcessWithRdServer(
                parent = parent
            ) {
                childProcessRunner.start(it)
            }
            logger.trace("rd process started")
            val proc = UtInstrumentationProcess(
                classLoader,
                rdProcess
            ).init()
//            Thread.sleep(300)
            logger.trace {"hearthbeatAlive - ${proc.protocol.wire.heartbeatAlive.value}, connected - ${
                proc.protocol.wire.connected.value
            }"}
            proc.lifetime.onTermination {
                logger.trace { "process is terminating" }
            }

            logger.trace("sending add paths")
            proc.request(
                Protocol.AddPathsCommand(
                    pathsToUserClasses,
                    pathsToDependencyClasses
                )
            )

            logger.trace("sending instrumentation")
            proc.request(Protocol.SetInstrumentationCommand(instrumentation))
            logger.trace("start commands sent")

            return proc
        }
    }

    private fun sendCommand(id: Long, cmd: Protocol.Command) {
        kryoHelper.writeCommand(id, cmd)
    }

    /**
     * Send command and instantly return
     *
     * @throws CancellationException process lifetime terminated and/or coroutine was cancelled
     */
    suspend fun request(cmd: Protocol.Command) {
        doCommand(cmd, awaitAnswer = false)
    }

    /**
     * Send command and wait answer from child process
     *
     * @return command from child process, possibly indicating operation failure
     * @throws CancellationException process lifetime terminated and/or coroutine was cancelled
     */
    suspend fun execute(cmd: Protocol.Command): Protocol.Command {
        return doCommand(cmd, awaitAnswer = true)!!
    }

    private suspend fun doCommand(cmd: Protocol.Command, awaitAnswer: Boolean = true): Protocol.Command? =
        lifetime.usingNested { operationLifetime ->
            // because some models use utcontext in toString method
            // utcontext is thread local, and so UtRdCoroutineScope does error
            // and not to pollute memory on no trace
            val cmdString: String? = if (logger.isTraceEnabled) cmd.toString() else null
            // if utbot cancels this job by timeout - await will throw cancellationException
            // also if process lifetime terminated - deferred from async call also cancelled and await throws cancellationException
            // if await succeeded - then process lifetime is ok and no timeout happened
            return com.jetbrains.rd.framework.util.withContext(
                operationLifetime,
                UtRdCoroutineScope.current.coroutineContext
            ) {
                val cmdId = ++lastSentId
                val deferred = CompletableDeferred<Protocol.Command>()

                try {
                    callbacks[cmdId] = deferred
                    operationLifetime.onTermination {
                        callbacks.remove(cmdId)?.run {
                            if (awaitAnswer) {
                                logger.trace { "operation $cmdId for $cmdString ended by timeout" }
                                deferred.cancel(CancellationException())
                            }
                        }
                    }
                    logger.trace { "Writing $cmdId, $cmdString to channel" }
                    sendCommand(cmdId, cmd)
                } catch (e: Throwable) { // means not sent
                    callbacks.remove(cmdId)?.let {
                        logger.trace { "operation $cmdId for cmd - $cmdString completed with exception: $e\nstacktrace - ${e.stackTraceToString()}" }
                        deferred.completeExceptionally(e)
                    }
                }

                return@withContext if (awaitAnswer) {
                    deferred.await()
                } else {
                    null
                }.apply { logger.trace { "operation $cmdId ended successfully for cmd - $cmdString, result - $this" } }
            }
        }

    private fun resumeWith(contId: Long, command: Protocol.Command) {
        logger.trace { "received: $contId | $command" }

        for (id in lastReceivedId + 1 until contId) {
            callbacks.remove(id)
                ?.complete(Protocol.ExceptionInChildProcess(java.lang.IllegalStateException("id $id was skipped in execution")))
        }

        // 1. if value is not null - we resume continuation with result
        // meaning request is completed successfully
        // 2. if value is null
        // this happens because operation lifetime terminated before we resume continuation
        callbacks.remove(contId)?.complete(command)
    }

    init {
        receiver =
            thread(
                name = "UtInstrumentationProcess-${rdProcess.process.pid}-receiver",
                isDaemon = true,
                start = false
            ) {
                while (lifetime.isAlive) {
                    try {
                        val (commandId, command) = kryoHelper.readCommand()

                        resumeWith(commandId, command)
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: CancellationException) {
                        // process lifetime terminated, all operations will be cancelled and remove from callback automatically
                        // new operations will not be accepted by lifetime
                        break
                    } catch (e: Throwable) {
                        if (e is ReadingFromKryoException && e.cause is InterruptedException)
                            break
                        // means we can't read somewhy
                        // kryo input buffer might be corrupted and any subsequent reads might fail or return incorrect data
                        // example of incorrect data might be incomplete strings
                        // buffer already filled, and we don't know how much it consumed
                        // all data read in kryo.input must be discarded
                        // also last received message from which buffer is filled should be also discarded
                        // because we send only complete messages - further messages should be ok
                        logger.warn { "Cant read from kryo - $e" }
                        kryoHelper.discard()
                    }
                }
            }

        lifetime.onTermination {
            logger.catch { receiver.interrupt() }
        }
    }
}