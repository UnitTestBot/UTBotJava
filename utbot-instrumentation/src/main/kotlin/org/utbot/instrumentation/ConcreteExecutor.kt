package org.utbot.instrumentation

import org.utbot.common.bracket
import org.utbot.common.catch
import org.utbot.common.currentThreadInfo
import org.utbot.common.debug
import org.utbot.common.pid
import org.utbot.common.trace
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.util.ChildProcessError
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.instrumentation.util.Protocol
import org.utbot.instrumentation.util.UnexpectedCommand
import java.io.Closeable
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.streams.toList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Creates [ConcreteExecutor], which delegates `execute` calls to the child process, and applies the given [block] to it.
 *
 * The child process will search for the classes in [pathsToUserClasses] and will use [instrumentation] for instrumenting.
 *
 * Specific instrumentation can add functionality to [ConcreteExecutor] via Kotlin extension functions.
 *
 * @param TIResult the return type of [Instrumentation.invoke] function for the given [instrumentation].
 * @return the result of the block execution on created [ConcreteExecutor].
 *
 * @see [org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation].
 */
inline fun <TBlockResult, TIResult, reified T : Instrumentation<TIResult>> withInstrumentation(
    instrumentation: T,
    pathsToUserClasses: String,
    pathsToDependencyClasses: String = ConcreteExecutor.defaultPathsToDependencyClasses,
    block: (ConcreteExecutor<TIResult, T>) -> TBlockResult
) = ConcreteExecutor(instrumentation, pathsToUserClasses, pathsToDependencyClasses).use {
    block(it)
}

class ConcreteExecutorPool(val maxCount: Int = Settings.defaultConcreteExecutorPoolSize) : AutoCloseable {
    private val executors = ArrayDeque<ConcreteExecutor<*, *>>(maxCount)

    /**
     * Tries to find the concrete executor for the supplied [instrumentation] and [pathsToDependencyClasses]. If it
     * doesn't exist, then creates a new one.
     */
    fun <TIResult, TInstrumentation : Instrumentation<TIResult>> get(
        instrumentation: TInstrumentation,
        pathsToUserClasses: String,
        pathsToDependencyClasses: String
    ): ConcreteExecutor<TIResult, TInstrumentation> {
        executors.removeIf { !it.alive }

        @Suppress("UNCHECKED_CAST")
        return executors.firstOrNull {
            it.pathsToUserClasses == pathsToUserClasses && it.instrumentation == instrumentation
        } as? ConcreteExecutor<TIResult, TInstrumentation>
            ?: ConcreteExecutor.createNew(instrumentation, pathsToUserClasses, pathsToDependencyClasses).apply {
                executors.addFirst(this)
                if (executors.size > maxCount) {
                    executors.removeLast().close()
                }
            }
    }

    override fun close() {
        executors.forEach { it.close() }
        executors.clear()
    }
}

/**
 * Concrete executor class. Takes [pathsToUserClasses] where the child process will search for the classes. Paths should
 * be separated with [java.io.File.pathSeparatorChar].
 *
 * If [instrumentation] depends on other classes, they should be passed in [pathsToDependencyClasses].
 *
 * Also takes [instrumentation] object which will be used in the child process for the instrumentation.
 *
 * @param TIResult the return type of [Instrumentation.invoke] function for the given [instrumentation].
 */
class ConcreteExecutor<TIResult, TInstrumentation : Instrumentation<TIResult>> private constructor(
    internal val instrumentation: TInstrumentation,
    internal val pathsToUserClasses: String,
    internal val pathsToDependencyClasses: String
) : Closeable, Executor<TIResult> {

    companion object {
        const val ERROR_CMD_ID = 0L
        var nextCommandId = 1L
        val defaultPool = ConcreteExecutorPool()
        var defaultPathsToDependencyClasses = ""


        var lastSendTimeMs = 0L
        var lastReceiveTimeMs = 0L

        /**
         * Delegates creation of the concrete executor to [defaultPool], which first searches for existing executor
         * and in case of failure, creates a new one.
         */
        operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String = defaultPathsToDependencyClasses
        ) = defaultPool.get(instrumentation, pathsToUserClasses, pathsToDependencyClasses)

        internal fun <TIResult, TInstrumentation : Instrumentation<TIResult>> createNew(
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
            pathsToDependencyClasses: String
        ) = ConcreteExecutor(instrumentation, pathsToUserClasses, pathsToDependencyClasses)
    }

    private val childProcessRunner = ChildProcessRunner()

    var classLoader: ClassLoader? = UtContext.currentContext()?.classLoader

    //property that signals to executors pool whether it can reuse this executor or not
    var alive = true
        private set

    /**
     * Executes [kCallable] in the child process with the supplied [arguments] and [parameters], e.g. static environment.
     *
     * @return the processed result of the method call.
     */
    override suspend fun executeAsync(
        kCallable: KCallable<*>,
        arguments: Array<Any?>,
        parameters: Any?
    ): TIResult {
        restartIfNeeded()

        val (clazz, signature) = when (kCallable) {
            is KFunction<*> -> kCallable.javaMethod?.run { declaringClass to signature }
                ?: kCallable.javaConstructor?.run { declaringClass to signature }
                ?: error("Not a constructor or a method")
            is KProperty<*> -> kCallable.javaGetter?.run { declaringClass to signature }
                ?: error("Not a getter")
            else -> error("Unknown KCallable: $kCallable")
        } // actually executableId implements the same logic, but it requires UtContext

        val invokeMethodCommand = Protocol.InvokeMethodCommand(
            clazz.name,
            signature,
            arguments.asList(),
            parameters
        )

        val cmdId = sendCommand(invokeMethodCommand)
        logger.trace("executeAsync, request: $cmdId , $invokeMethodCommand")

        try {
            val res = awaitCommand<Protocol.InvocationResultCommand<*>, TIResult>(cmdId) {
                @Suppress("UNCHECKED_CAST")
                it.result as TIResult
            }
            logger.trace { "executeAsync, response: $cmdId, $res" }
            return res
        } catch (e: Throwable) {
            logger.trace { "executeAsync, response(ERROR): $cmdId, $e" }
            throw  e
        }
    }

    /**
     * Send command and return its sequential_id
     */
    private fun sendCommand(cmd: Protocol.Command): Long {
        lastSendTimeMs = System.currentTimeMillis()

        val kryoHelper = state?.kryoHelper ?: error("State is null")

        val res = nextCommandId++
        logger.trace().bracket("Writing $res, $cmd to channel") {
            kryoHelper.writeCommand(res, cmd)
        }
        return res
    }

    fun warmup() {
        restartIfNeeded()
        sendCommand(Protocol.WarmupCommand())
    }

    /**
     * Restarts the child process if it is not active.
     */
    class ConnectorState(
        val receiverThread: Thread,
        val process: Process,
        val kryoHelper: KryoHelper,
        val receiveChannel: Channel<CommandResult>
    ) {
        var disposed = false
    }

    data class CommandResult(
        val commandId: Long,
        val command: Protocol.Command,
        val processStdout: InputStream
    )

    var state: ConnectorState? = null

    private fun restartIfNeeded() {
        val oldState = state
        if (oldState != null && oldState.process.isAlive && !oldState.disposed) return

        logger.debug()
            .bracket("restartIfNeeded; instrumentation: '${instrumentation.javaClass.simpleName}',classpath: '$pathsToUserClasses'") {
                //stop old thread
                oldState?.terminateResources()

                try {
                    val process = childProcessRunner.start()
                    val kryoHelper = KryoHelper(
                        process.inputStream ?: error("Can't get the standard output of the subprocess"),
                        process.outputStream ?: error("Can't get the standard input of the subprocess")
                    )
                    classLoader?.let { kryoHelper.setKryoClassLoader(it) }

                    val readCommandsChannel = Channel<CommandResult>(capacity = Channel.UNLIMITED)
                    val receiverThread =
                        thread(name = "ConcreteExecutor-${process.pid}-receiver", isDaemon = true, start = false) {
                            val s = state!!
                            while (true) {
                                val cmd = try {
                                    val (commandId, command) = kryoHelper.readLong() to kryoHelper.readCommand()
                                    logger.trace { "receiver: readFromStream: $commandId : $command" }
                                    CommandResult(commandId, command, process.inputStream)
                                } catch (e: Throwable) {
                                    if (s.disposed) {
                                        break
                                    }

                                    s.disposed = true
                                    CommandResult(
                                        ERROR_CMD_ID,
                                        Protocol.ExceptionInKryoCommand(e),
                                        process.inputStream
                                    )
                                } finally {
                                    lastReceiveTimeMs = System.currentTimeMillis()
                                }

                                try {
                                    readCommandsChannel.offer(cmd)
                                } catch (e: CancellationException) {
                                    s.disposed = true

                                    logger.info(e) { "Receiving is canceled in thread ${currentThreadInfo()} while sending command: $cmd" }
                                    break
                                } catch (e: Throwable) {
                                    s.disposed = true

                                    logger.error(e) { "Unexpected error while sending to channel in ${currentThreadInfo()}, cmd=$cmd" }
                                    break
                                }
                            }
                        }

                    state = ConnectorState(receiverThread, process, kryoHelper, readCommandsChannel)
                    receiverThread.start()

                    // send classpath
                    // we don't expect ProcessReadyCommand here
                    sendCommand(
                        Protocol.AddPathsCommand(
                            pathsToUserClasses,
                            pathsToDependencyClasses
                        )
                    )

                    // send instrumentation
                    // we don't expect ProcessReadyCommand here
                    sendCommand(Protocol.SetInstrumentationCommand(instrumentation))

                } catch (e: Throwable) {
                    state?.terminateResources()
                    throw  e
                }
            }

    }

    /**
     * Sends [requestCmd] to the ChildProcess.
     * If [action] is not null, waits for the response command, performs [action] on it and returns the result.
     * This function is helpful for creating extensions for specific instrumentations.
     * @see [org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation].
     */
    fun <T : Protocol.Command, R> request(requestCmd: T, action: ((Protocol.Command) -> R)): R = runBlocking {
        restartIfNeeded()
        awaitCommand(sendCommand(requestCmd), action)
    }

    /**
     * Read next command of type [T]  or throw exception
     */
    private suspend inline fun <reified T : Protocol.Command, R> awaitCommand(
        awaitingCmdId: Long,
        action: (T) -> R
    ): R {
        val s = state ?: error("State is not initialized")

        if (!currentCoroutineContext().isActive) {
            logger.warn { "Current coroutine is canceled" }
        }

        while (true) {
            val (receivedId, cmd, processStdout) = s.receiveChannel.receive()

            if (receivedId == awaitingCmdId || receivedId == ERROR_CMD_ID) {
                return when (cmd) {
                    is T -> action(cmd)
                    is Protocol.ExceptionInChildProcess -> throw ChildProcessError(cmd.exception)
                    is Protocol.ExceptionInKryoCommand -> {
                        // we assume that exception in Kryo means child process death
                        // and we do not need to check is it alive
                        throw ConcreteExecutionFailureException(
                            cmd.exception,
                            childProcessRunner.errorLogFile,
                            processStdout.bufferedReader().lines().toList()
                        )
                    }
                    else -> throw UnexpectedCommand(cmd)
                }
            } else if (receivedId > awaitingCmdId) {
                logger.error { "BAD: Awaiting id: $awaitingCmdId, received: $receivedId" }
                throw UnexpectedCommand(cmd)
            }
        }
    }

    // this fun sometimes work improperly - process dies after delay
    @Suppress("unused")
    private suspend fun checkProcessIsDeadWithTimeout(): Boolean =
        if (state?.process?.isAlive == false) {
            true
        } else {
            delay(50)
            state?.process?.isAlive == false
        }

    private fun ConnectorState.terminateResources() {
        if (disposed)
            return

        disposed = true
        logger.debug { "Terminating resources in ConcreteExecutor.connectorState" }

        if (!process.isAlive) {
            return
        }
        logger.catch { kryoHelper.writeCommand(nextCommandId++, Protocol.StopProcessCommand()) }
        logger.catch { kryoHelper.close() }

        logger.catch { receiveChannel.close() }

        logger.catch { process.waitUntilExitWithTimeout() }
    }

    override fun close() {
        state?.terminateResources()
        alive = false
    }
}

private fun Process.waitUntilExitWithTimeout() {
    try {
        if (!waitFor(100, TimeUnit.MICROSECONDS)) {
            destroyForcibly()
        }
    } catch (e: Throwable) {
        logger.error(e) { "Error during termination of child process" }
    }
}