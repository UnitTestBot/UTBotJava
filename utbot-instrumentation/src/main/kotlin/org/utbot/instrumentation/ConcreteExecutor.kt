package org.utbot.instrumentation

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.isNotAlive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.process.ChildProcessRunner
import org.utbot.instrumentation.rd.UtInstrumentationProcess
import org.utbot.instrumentation.util.ChildProcessError
import org.utbot.instrumentation.util.Protocol
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.streams.toList

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
    private val def: LifetimeDefinition = LifetimeDefinition()
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
    var alive = true
        private set

    private val corMutex = Mutex()
    private var processInstance: UtInstrumentationProcess? = null

    private suspend fun regenerate() {
        val proc = processInstance

        if (proc == null || proc.lifetime.isNotAlive) {
            processInstance = UtInstrumentationProcess(
                def.createNested(),
                childProcessRunner,
                instrumentation,
                pathsToUserClasses,
                pathsToDependencyClasses,
                classLoader
            )
        }
    }

    private suspend inline fun <T> withProcess(block: UtInstrumentationProcess.() -> T): T {
        corMutex.withLock {
            regenerate()
        }

        return processInstance!!.block()
    }

    private suspend inline fun <T> withProcessExclusively(block: UtInstrumentationProcess.() -> T): T {
        corMutex.withLock {
            regenerate()
            return processInstance!!.block()
        }
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
        val (clazz, signature) = when (kCallable) {
            is KFunction<*> -> kCallable.javaMethod?.run { declaringClass to signature }
                ?: kCallable.javaConstructor?.run { declaringClass to signature }
                ?: error("Not a constructor or a method")
            is KProperty<*> -> kCallable.javaGetter?.run { declaringClass to signature }
                ?: error("Not a getter")
            else -> error("Unknown KCallable: $kCallable")
        } // actually executableId implements the same logic, but it requires UtContext

        try {
            val cmd = Protocol.InvokeMethodCommand(clazz.name, signature, arguments.asList(), parameters)

            return (execute(cmd) as Protocol.InvocationResultCommand<TIResult>).result
        } catch (e: Throwable) {
            logger.trace { "executeAsync, response(ERROR): $e" }
            throw e
        }
    }

    suspend fun <T : Protocol.Command> execute(cmd: T): Protocol.Command = withProcess {
        try {
            sendTimestamp.set(System.currentTimeMillis())
            return when (val result = execute(cmd)) {
                is Protocol.ExceptionInChildProcess -> {
                    if (lifetime.isAlive)
                        throw ChildProcessError(result.exception)
                    else
                        throw ConcreteExecutionFailureException(
                            result.exception,
                            childProcessRunner.errorLogFile,
                            process.inputStream.bufferedReader().lines().toList()
                        )
                }
                else -> result
            }
        } catch (e: CancellationException) {
            if (lifetime.isNotAlive)
                throw ConcreteExecutionFailureException(
                    e,
                    childProcessRunner.errorLogFile,
                    process.inputStream.bufferedReader().lines().toList()
                )
            else
                throw e
        } finally {
            receiveTimeStamp.set(System.currentTimeMillis())
        }
    }

    suspend fun <T : Protocol.Command> request(cmd: T) = withProcess {
        sendTimestamp.set(System.currentTimeMillis())
        request(cmd)
    }

    override fun close() {
        if (alive && def.isAlive) {
            alive = false
            def.executeIfAlive {
                runBlocking {
                    request(Protocol.StopProcessCommand())
                }
            }
            def.terminate()
        }
    }
}

fun ConcreteExecutor<*,*>.warmup() = runBlocking {
    request(Protocol.WarmupCommand())
}