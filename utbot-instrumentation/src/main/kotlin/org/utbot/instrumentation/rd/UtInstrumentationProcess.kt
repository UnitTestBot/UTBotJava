package org.utbot.instrumentation.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.utbot.common.*
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val logger = KotlinLogging.logger("UtInstrumentationProcess")

/**
 * Main goals of this class:
 * 1. prepare started child process for execution - sending paths and instrumentation
 * 2. communicate with child process, i.e. send and receive messages
 *
 * facts
 * 1. process lifetime - helps indicate that child died
 * 2. operation lifetime - terminates either by process lifetime or coroutine scope cancellation, i.e. timeout
 * also removes orphaned continuation on termination
 * 3. child process operations are executed consequently and must always return exactly one answer command
 * 4. if process is dead - it always throws CancellationException on any operation
 */

/**
 * do not allow to obtain dead process, return newly restart instance it if terminated
 *
 * there are 2 ways of communicating with process
 * 1. pessimistic
 *      - operation can be executed only and if only previous operation was completed
 *      - process cannot be obtained until any operation is executing
 *
 * 2. optimistic
 *      - process can be obtained if it's lifetime.isAlive
 *      - operations are always sent
 *      - if there are many pending operation and process dies - all operations are cancelled
 *
 * in optimistic situation on command fail all subsequent sent operations will not receive answer and will not be re-executed
 * this is because operation fail nearly ALWAYS crash child process
 * so pessimistic way solves it
 *
 * corner cases for optimistic way:
 * we need to order multiple coroutines waiting for answer after they sent command
 * fair channel is a go
 * but there are commands that does not answer but still might cause answer and fair channel would will fail
 * also while(true) is kek realization
 * One correct realization is to manually suspend and register continuation in some map
 * NOTE: we first register and then send!
 * then there will in separate process read everything and then resume callback
 * and pessimitic locking is solved in concrete executor
 *
 *
 * 3. may be we need process acknowledgement of operation -
 * 4. may be we need cancellation on child process - if we cancel operation on our side then we need to stop same operation on child process
 *
 * 6. operations must be executed on a RdCoroutineScope - also the problem is that we may write to kryo simulatneously - solved by coroutines, kryo writing should not be cancellable
 * 7. operations executed in FIFO - command-answer | command-answer, so if child process is busy
 */
class UtInstrumentationProcess private constructor(
    parent: Lifetime,
    private val classLoader: ClassLoader?,
    private val rdProcess: ProcessWithRdServer
) : ProcessWithRdServer by rdProcess {
    private var lastSent = 0L
    private var lastReceived = 0L
    private val callbacks: ConcurrentMap<Long, Continuation<Protocol.Command>> = ConcurrentSkipListMap()
    private val kryoHelper =
        KryoHelper(rdProcess.lifetime, rdProcess.protocol.scheduler, rdProcess.fromProcess, rdProcess.toProcess) { logger.info(it) }.apply {
            classLoader?.let { setKryoClassLoader(it) }
        }

    companion object {
        suspend operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            parent: Lifetime,
            childProcessRunner: ChildProcessRunner,
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String,
            classLoader: ClassLoader?,
            rdProcess: ProcessWithRdServer = UtRdUtil.startUtProcessWithRdServer(
                parent = parent
            ) {
                childProcessRunner.start(it)
            }
        ): UtInstrumentationProcess {
            val proc = UtInstrumentationProcess(
                parent,
                classLoader,
                rdProcess
            )
            proc.lifetime.onTermination {
                logger.info { "process is terminating" }
            }

            logger.info("sending add paths")
            proc.executeCommand(
                Protocol.AddPathsCommand(
                    pathsToUserClasses,
                    pathsToDependencyClasses
                )
            )

            logger.info("sending instrumentation")
            proc.executeCommand(Protocol.SetInstrumentationCommand(instrumentation))
            logger.info("start commands sent")

            return proc
        }
    }

    private fun sendCommand(id: Long, cmd: Protocol.Command) {
        logger.info { "Writing $id, $cmd to channel" }

        kryoHelper.writeCommand(id, cmd)
    }

    /**
     * Sends provided command and returns command answered by child process
     *
     * This method either:
     *  1. returns some command, possibly indicating operation failure
     *  2. throws CancellationException because process lifetime terminated and/or coroutine was cancelled
     *  3. throws WritingToKryoException if some problems with kryo arose. In that case no message is sent
     */
    suspend fun executeCommand(cmd: Protocol.Command): Protocol.Command = lifetime.usingNested { operationLifetime ->
        // if utbot cancels this job by timeout - await will throw cancellationException
        // also if process lifetime terminated - deferred from async call also cancelled and await throws cancellationException
        // if await succeeded - then process lifetime is ok and no timeout happened
        try {
            operationLifetime.onTermination {
                logger.info {
                "operatoin is terminated"
                }
            }
            val result = UtRdCoroutineScope.current.async(operationLifetime) {
                logger.info { "before suspend" }
                try {
                    suspendCoroutine<Protocol.Command> { cont ->
                        try {
                            val cmdId = ++lastSent

                            callbacks[cmdId] = cont
                            operationLifetime.onTermination {
                                logger.catch {
                                    logger.info {"terminating lifeitme"}
                                    callbacks.remove(cmdId)
                                        ?.resumeWithException(CancellationException("operation $cmdId lifetime terminated"))
                                }
                            }
                            sendCommand(cmdId, cmd)
                        } catch (e: Throwable) { // means not sent
                            logger.info {
                                "terminating lifetime 2"
                            }
                            callbacks.remove(lastSent--)
                            cont.resumeWithException(e)
                        }
                    }
                }
                catch (e: Throwable) {
                    logger.info { "suspend failed: $e" }
                    throw e
                }
                finally {
                    logger.info { "suspend done" }
                }
            }.await()

            return result
        }
        catch(e: Throwable) {
            logger.info {"exception before await: $e"}
            throw e
        }
        finally {
            logger.info { "ending result check: proc - ${lifetime.isAlive}, operation - ${operationLifetime.isAlive}"}
        }
    }

    private fun resumeWith(contId: Long, command: Protocol.Command) {
        ++lastReceived
        logger.info { "received $lastReceived: $contId | $command" }
        // because child process must process request sequentially
        // and answer on every message
        require(lastReceived == contId)

        // 1. if value is not null - we resume continuation with result
        // meaning request is completed successfully
        // 2. if value is null
        // this happens because operation lifetime terminated before we resume continuation
        callbacks.remove(contId)?.resume(command)
    }

    init {
        val receiver =
            thread(name = "UtInstrumentationProcess-${rdProcess.pid}-receiver", isDaemon = true, start = true) {
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
                    } catch (e: ReadingFromKryoException) {
                        // means we can't read somewhy
                        // the problem is that kryo input buffer might be corrupted and any subsequent reads might fail or return incorrect data
                        // example of incorrect data might be incomplete strings
                        // buffer already filled, and we don't know how much it consumed
                        // all data read in kryo.input must be discarded
                        // also last received message from which buffer is filled should be also discarded
                        // because we send only complete messages - other messages should be ok
                        logger.warn { e.toString() }
                        resumeWith(lastReceived + 1, Protocol.ExceptionInChildProcess(e))
                        kryoHelper.discard()
                    } catch (e: Throwable) {
                        // should be impossible, there are no other exceptions to be thrown in that code
                        logger.error("should be impossible, something is very very bad", e)
                    }
                }
            }

        lifetime.onTermination {
            logger.catch { receiver.interrupt() } // TODO shall i?
        }
    }
}