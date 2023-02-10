package org.utbot.instrumentation

import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong
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
import org.utbot.framework.plugin.api.InstrumentedProcessDeathException
import org.utbot.common.logException
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.InstrumentedProcessRunner
import org.utbot.instrumentation.process.generated.ComputeStaticFieldParams
import org.utbot.instrumentation.process.generated.InvokeMethodCommandParams
import org.utbot.instrumentation.rd.InstrumentedProcess
import org.utbot.instrumentation.util.InstrumentedProcessError
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.rd.loggers.overrideDefaultRdLoggerFactoryWithKLogger

private val logger = KotlinLogging.logger {}

/**
 * Creates [ConcreteExecutor], which delegates `execute` calls to the instrumented process, and applies the given [block] to it.
 *
 * The instrumented process will search for the classes in [pathsToUserClasses] and will use [instrumentation] for instrumenting.
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
    block: (ConcreteExecutor<TIResult, T>) -> TBlockResult
) = ConcreteExecutor(instrumentation, pathsToUserClasses).use {
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
    ): ConcreteExecutor<TIResult, TInstrumentation> {
        executors.removeIf { !it.alive }

        @Suppress("UNCHECKED_CAST")
        return executors.firstOrNull {
            it.pathsToUserClasses == pathsToUserClasses && it.instrumentation == instrumentation
        } as? ConcreteExecutor<TIResult, TInstrumentation>
            ?: ConcreteExecutor.createNew(instrumentation, pathsToUserClasses).apply {
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
 * Concrete executor class. Takes [pathsToUserClasses] where the instrumented process will search for the classes. Paths should
 * be separated with [java.io.File.pathSeparatorChar].
 *
 * If [instrumentation] depends on other classes, they should be passed in [pathsToDependencyClasses].
 *
 * Also takes [instrumentation] object which will be used in the instrumented process for the instrumentation.
 *
 * @param TIResult the return type of [Instrumentation.invoke] function for the given [instrumentation].
 */
class ConcreteExecutor<TIResult, TInstrumentation : Instrumentation<TIResult>> private constructor(
    internal val instrumentation: TInstrumentation,
    internal val pathsToUserClasses: String
) : Closeable, Executor<TIResult> {
    private val ldef: LifetimeDefinition = LifetimeDefinition()
    private val instrumentedProcessRunner: InstrumentedProcessRunner = InstrumentedProcessRunner()

    companion object {

        private val sendTimestamp = AtomicLong()
        private val receiveTimeStamp = AtomicLong()
        val lastSendTimeMs: Long
            get() = sendTimestamp.get()
        val lastReceiveTimeMs: Long
            get() = receiveTimeStamp.get()
        val defaultPool = ConcreteExecutorPool()

        init {
            overrideDefaultRdLoggerFactoryWithKLogger(logger)
        }

        /**
         * Delegates creation of the concrete executor to [defaultPool], which first searches for existing executor
         * and in case of failure, creates a new one.
         */
        operator fun <TIResult, TInstrumentation : Instrumentation<TIResult>> invoke(
            instrumentation: TInstrumentation,
            pathsToUserClasses: String,
        ) = defaultPool.get(instrumentation, pathsToUserClasses)

        internal fun <TIResult, TInstrumentation : Instrumentation<TIResult>> createNew(
            instrumentation: TInstrumentation,
            pathsToUserClasses: String
        ) = ConcreteExecutor(instrumentation, pathsToUserClasses)
    }

    var classLoader: ClassLoader? = UtContext.currentContext()?.classLoader

    //property that signals to executors pool whether it can reuse this executor or not
    val alive: Boolean
        get() = ldef.isAlive

    private val corMutex = Mutex()
    private var processInstance: InstrumentedProcess? = null

    // this function is intended to be called under corMutex
    private suspend fun regenerate(): InstrumentedProcess {
        ldef.throwIfNotAlive()

        var proc: InstrumentedProcess? = processInstance

        if (proc == null || !proc.lifetime.isAlive) {
            proc = InstrumentedProcess(
                ldef,
                instrumentedProcessRunner,
                instrumentation,
                pathsToUserClasses,
                classLoader
            )
            processInstance = proc
        }

        return proc
    }

    /**
     * Main entry point for communicating with instrumented process.
     * Use this function every time you want to access protocol model.
     * This method prepares instrumented process for execution and ensures it is alive before giving it block
     *
     * @param exclusively if true - executes block under mutex.
     * This guarantees that no one can access protocol model - no other calls made before block completes
     */
    suspend fun <T> withProcess(exclusively: Boolean = false, block: suspend InstrumentedProcess.() -> T): T {
        fun throwConcreteIfDead(e: Throwable, proc: InstrumentedProcess?) {
            if (proc?.lifetime?.isAlive != true) {
                throw InstrumentedProcessDeathException(e)
            }
        }

        sendTimestamp.set(System.currentTimeMillis())

        var proc: InstrumentedProcess? = null

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
            // this clearly indicates instrumented process death -> ConcreteExecutionFailureException
            throwConcreteIfDead(e, proc)
            // 2. it can be ordinary timeout from coroutine. then just rethrow
            throw e
        }
        catch(e: Throwable) {
            // after exception process can either
            // 1. be dead because of this exception
            throwConcreteIfDead(e, proc)
            // 2. might be deliberately thrown and process still can operate
            throw InstrumentedProcessError(e)
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
    ): TIResult = logger.logException("executeAsync, response(ERROR)") {
        withProcess {
            val argumentsByteArray = kryoHelper.writeObject(arguments.asList())
            val parametersByteArray = kryoHelper.writeObject(parameters)
            val params = InvokeMethodCommandParams(className, signature, argumentsByteArray, parametersByteArray)

            val result = instrumentedProcessModel.invokeMethodCommand.startSuspending(lifetime, params).result
            kryoHelper.readObject(result)
        }
    }

    /**
     * Executes [kCallable] in the instrumented process with the supplied [arguments] and [parameters], e.g. static environment.
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
                            protocol.synchronizationModel.stopProcess.fire(Unit)
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
        instrumentedProcessModel.warmup.start(lifetime, Unit)
    }
}

/**
 * Extension function for the [ConcreteExecutor], which allows to collect static field value of [fieldId].
 */
fun <T> ConcreteExecutor<*, *>.computeStaticField(fieldId: FieldId): Result<T> = runBlocking {
    withProcess {
        val fieldIdSerialized = kryoHelper.writeObject(fieldId)
        val params = ComputeStaticFieldParams(fieldIdSerialized)

        val result = instrumentedProcessModel.computeStaticField.startSuspending(lifetime, params)

        kryoHelper.readObject(result.result)
    }
}
