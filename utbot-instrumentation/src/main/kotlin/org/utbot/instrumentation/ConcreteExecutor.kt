package org.utbot.instrumentation

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.streams.toList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.rd.UtInstrumentationProcess
import org.utbot.instrumentation.rd.generated.ComputeStaticFieldParams
import org.utbot.instrumentation.rd.generated.InvokeMethodCommandParams
import org.utbot.instrumentation.util.ChildProcessError
import org.utbot.rd.loggers.UtRdKLoggerFactory

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
            it.pathsToUserClasses == pathsToUserClasses && it.instrumentation == instrumentation && it.pathsToDependencyClasses == pathsToDependencyClasses
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

    fun forceTerminateProcesses() {
        executors.forEach {
            it.forceTerminateProcess()
        }
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
    private val ldef: LifetimeDefinition = LifetimeDefinition()
    private val childProcessRunner: ChildProcessRunner = ChildProcessRunner()

    companion object {

        private val sendTimestamp = AtomicLong()
        private val receiveTimeStamp = AtomicLong()
        val lastSendTimeMs: Long
            get() = sendTimestamp.get()
        val lastReceiveTimeMs: Long
            get() = receiveTimeStamp.get()
        val defaultPool = ConcreteExecutorPool()
        var defaultPathsToDependencyClasses = ""

        init {
            Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))
            Runtime.getRuntime().addShutdownHook(thread(start = false) { defaultPool.close() })
        }

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

    var classLoader: ClassLoader? = UtContext.currentContext()?.classLoader

    //property that signals to executors pool whether it can reuse this executor or not
    val alive: Boolean
        get() = ldef.isAlive

    private val corMutex = Mutex()
    private var processInstance: UtInstrumentationProcess? = null

    // this function is intended to be called under corMutex
    private suspend fun regenerate(): UtInstrumentationProcess {
        ldef.throwIfNotAlive()

        var proc: UtInstrumentationProcess? = processInstance

        if (proc == null || !proc.lifetime.isAlive) {
            proc = UtInstrumentationProcess(
                ldef,
                childProcessRunner,
                instrumentation,
                pathsToUserClasses,
                pathsToDependencyClasses,
                classLoader
            )
            processInstance = proc
        }

        return proc
    }

    /**
     * Main entry point for communicating with child process.
     * Use this function every time you want to access protocol model.
     * This method prepares child process for execution and ensures it is alive before giving it block
     *
     * @param exclusively if true - executes block under mutex.
     * This guarantees that no one can access protocol model - no other calls made before block completes
     */
    suspend fun <T> withProcess(exclusively: Boolean = false, block: suspend UtInstrumentationProcess.() -> T): T {
        fun throwConcreteIfDead(e: Throwable, proc: UtInstrumentationProcess?) {
            if (proc?.lifetime?.isAlive != true) {
                throw ConcreteExecutionFailureException(e,
                    childProcessRunner.errorLogFile,
                    try {
                        proc?.run { process.inputStream.bufferedReader().lines().toList() } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                )
            }
        }

        sendTimestamp.set(System.currentTimeMillis())

        var proc: UtInstrumentationProcess? = null

        try {
            if (exclusively) {
                corMutex.withLock {
                    proc = regenerate()
                    return proc!!.block()
                }
            }
            else {
                return corMutex.withLock { regenerate().apply { proc = this } }.block()
            }
        }
        catch (e: CancellationException) {
            // cancellation can be from 2 causes
            // 1. process died, its lifetime terminated, so operation was cancelled
            // this clearly indicates child process death -> ConcreteExecutionFailureException
            throwConcreteIfDead(e, proc)
            // 2. it can be ordinary timeout from coroutine. then just rethrow
            throw e
        }
        catch(e: Throwable) {
            // after exception process can either
            // 1. be dead because of this exception
            throwConcreteIfDead(e, proc)
            // 2. might be deliberately thrown and process still can operate
            throw ChildProcessError(e)
        }
        finally {
            receiveTimeStamp.set(System.currentTimeMillis())
        }
    }

    suspend fun executeAsync(
        className: String,
        signature: String,
        arguments: Array<Any?>,
        parameters: Any?
    ): TIResult = try {
        withProcess {
            val argumentsByteArray = kryoHelper.writeObject(arguments.asList())
            val parametersByteArray = kryoHelper.writeObject(parameters)
            val params = InvokeMethodCommandParams(className, signature, argumentsByteArray, parametersByteArray)

            val ba = protocolModel.invokeMethodCommand.startSuspending(lifetime, params).result
            kryoHelper.readObject(ba)
        }
    } catch (e: Throwable) {
        logger.trace { "executeAsync, response(ERROR): $e" }
        throw e
    }

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
        val (className, signature) = when (kCallable) {
            is KFunction<*> -> kCallable.javaMethod?.run { declaringClass.name to signature }
                ?: kCallable.javaConstructor?.run { declaringClass.name to signature }
                ?: error("Not a constructor or a method")
            is KProperty<*> -> kCallable.javaGetter?.run { declaringClass.name to signature }
                ?: error("Not a getter")
            else -> error("Unknown KCallable: $kCallable")
        } // actually executableId implements the same logic, but it requires UtContext

        return executeAsync(className, signature, arguments, parameters)
    }

    override fun close() {
        forceTerminateProcess()
    }

    fun forceTerminateProcess() {
        runBlocking {
            corMutex.withLock {
                if (alive) {
                    try {
                        processInstance?.run {
                            protocolModel.stopProcess.start(lifetime, Unit)
                        }
                    } catch (_: Exception) {}
                    processInstance = null
                }
                ldef.terminate()
            }
        }
    }

}

fun ConcreteExecutor<*,*>.warmup() = runBlocking {
    withProcess {
        protocolModel.warmup.start(lifetime, Unit)
    }
}

/**
 * Extension function for the [ConcreteExecutor], which allows to collect static field value of [fieldId].
 */
fun <T> ConcreteExecutor<*, *>.computeStaticField(fieldId: FieldId): Result<T> = runBlocking {
    withProcess {
        val fieldIdSerialized = kryoHelper.writeObject(fieldId)
        val params = ComputeStaticFieldParams(fieldIdSerialized)

        val result = protocolModel.computeStaticField.startSuspending(lifetime, params)

        kryoHelper.readObject(result.result)
    }
}
