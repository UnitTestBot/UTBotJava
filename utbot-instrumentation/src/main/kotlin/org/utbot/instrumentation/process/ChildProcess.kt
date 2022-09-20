package org.utbot.instrumentation.process

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.defaultLogFormat
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.plusAssign
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.utbot.common.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.agent.Agent
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.rd.childCreatedFileName
import org.utbot.instrumentation.rd.generated.CollectCoverageResult
import org.utbot.instrumentation.rd.generated.InvokeMethodCommandResult
import org.utbot.instrumentation.rd.generated.ProtocolModel
import org.utbot.instrumentation.rd.obtainClientIO
import org.utbot.instrumentation.rd.processSyncDirectory
import org.utbot.instrumentation.rd.signalChildReady
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.UtRdCoroutineScope
import org.utbot.rd.adviseForConditionAsync
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.security.AllPermission
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import org.utbot.framework.plugin.api.FieldId
import org.utbot.instrumentation.rd.generated.ComputeStaticFieldResult

/**
 * We use this ClassLoader to separate user's classes and our dependency classes.
 * Our classes won't be instrumented.
 */
private object HandlerClassesLoader : URLClassLoader(emptyArray()) {
    fun addUrls(urls: Iterable<String>) {
        urls.forEach { super.addURL(File(it).toURI().toURL()) }
    }

    /**
     * System classloader can find org.slf4j thus when we want to mock something from org.slf4j
     * we also want this class will be loaded by [HandlerClassesLoader]
     */
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("org.slf4j")) {
            return (findLoadedClass(name) ?: findClass(name)).apply {
                if (resolve) resolveClass(this)
            }
        }
        return super.loadClass(name, resolve)
    }
}

private typealias ChildProcessLogLevel = LogLevel
private val logLevel = ChildProcessLogLevel.Info

// Logging
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
private inline fun log(level: ChildProcessLogLevel, any: () -> Any?) {
    if (level < logLevel)
        return

    System.err.println(LocalDateTime.now().format(dateFormatter) + " ${level.name.uppercase()}| ${any()}")
}

// errors that must be address
internal inline fun logError(any: () -> Any?) {
    log(ChildProcessLogLevel.Error, any)
}

// default log level for irregular useful messages that does not pollute log
internal inline fun logInfo(any: () -> Any?) {
    log(ChildProcessLogLevel.Info, any)
}

// log level for frequent messages useful for debugging
internal inline fun logDebug(any: () -> Any?) {
    log(ChildProcessLogLevel.Debug, any)
}

// log level for internal rd logs and frequent messages
// heavily pollutes log, useful only when debugging rpc
// probably contains no info about utbot
internal fun logTrace(any: () -> Any?) {
    log(ChildProcessLogLevel.Trace, any)
}

private enum class State {
    STARTED,
    ENDED
}

private val messageFromMainTimeoutMillis: Long = TimeUnit.SECONDS.toMillis(120)
private val synchronizer: Channel<State> = Channel(1)

/**
 * Command-line option to disable the sandbox
 */
const val DISABLE_SANDBOX_OPTION = "--disable-sandbox"

/**
 * It should be compiled into separate jar file (child_process.jar) and be run with an agent (agent.jar) option.
 */
suspend fun main(args: Array<String>) = runBlocking {
    if (!args.contains(DISABLE_SANDBOX_OPTION)) {
        permissions {
            // Enable all permissions for instrumentation.
            // SecurityKt.sandbox() is used to restrict these permissions.
            +AllPermission()
        }
    }
    // 0 - auto port for server, should not be used here
    val port = args.find { it.startsWith(serverPortProcessArgumentTag) }
        ?.run { split("=").last().toInt().coerceIn(1..65535) }
        ?: throw IllegalArgumentException("No port provided")

    val pid = currentProcessPid.toInt()
    val def = LifetimeDefinition()

    launch {
        var lastState = State.STARTED
        while (true) {
            val current: State? =
                withTimeoutOrNull(messageFromMainTimeoutMillis) {
                    synchronizer.receive()
                }
            if (current == null) {
                if (lastState == State.ENDED) {
                    // process is waiting for command more than expected, better die
                    logInfo { "terminating lifetime" }
                    def.terminate()
                    break
                }
            }
            else {
                lastState = current
            }
        }
    }

    def.usingNested { lifetime ->
        lifetime += { logInfo { "lifetime terminated" } }
        try {
            logInfo {"pid - $pid"}
            logInfo {"isJvm8 - $isJvm8, isJvm9Plus - $isJvm9Plus, isWindows - $isWindows"}
            initiate(lifetime, port, pid)
        } finally {
            val syncFile = File(processSyncDirectory, childCreatedFileName(pid))

            if (syncFile.exists()) {
                logInfo { "sync file existed" }
                syncFile.delete()
            }
        }
    }
}

private fun <T> measureExecutionForTermination(block: () -> T): T = runBlocking {
    try {
        synchronizer.send(State.STARTED)
        return@runBlocking block()
    }
    finally {
        synchronizer.send(State.ENDED)
    }
}

private lateinit var pathsToUserClasses: Set<String>
private lateinit var pathsToDependencyClasses: Set<String>
private lateinit var instrumentation: Instrumentation<*>

private fun <T, R> RdCall<T, R>.measureExecutionForTermination(block: (T) -> R) {
    this.set { it ->
        runBlocking {
            measureExecutionForTermination<R> {
                try {
                    block(it)
                } catch (e: Throwable) {
                    logError { e.stackTraceToString() }
                    throw e
                }
            }
        }
    }
}

private fun ProtocolModel.setup(kryoHelper: KryoHelper, onStop: () -> Unit) {
    warmup.measureExecutionForTermination {
        logDebug { "received warmup request" }
        val time = measureTimeMillis {
            HandlerClassesLoader.scanForClasses("").toList() // here we transform classes
        }
        logDebug { "warmup finished in $time ms" }
    }
    invokeMethodCommand.measureExecutionForTermination { params ->
        logDebug { "received invokeMethod request: ${params.classname}, ${params.signature}" }
        val clazz = HandlerClassesLoader.loadClass(params.classname)
        val res = instrumentation.invoke(
            clazz,
            params.signature,
            kryoHelper.readObject(params.arguments),
            kryoHelper.readObject(params.parameters)
        )

        logDebug { "invokeMethod result: $res" }
        InvokeMethodCommandResult(kryoHelper.writeObject(res))
    }
    setInstrumentation.measureExecutionForTermination { params ->
        logDebug { "setInstrumentation request" }
        instrumentation = kryoHelper.readObject(params.instrumentation)
        logTrace { "instrumentation - ${instrumentation.javaClass.name} " }
        Agent.dynamicClassTransformer.transformer = instrumentation // classTransformer is set
        Agent.dynamicClassTransformer.addUserPaths(pathsToUserClasses)
        instrumentation.init(pathsToUserClasses)
    }
    addPaths.measureExecutionForTermination { params ->
        logDebug { "addPaths request" }
        logTrace { "path to userClasses - ${params.pathsToUserClasses}"}
        logTrace { "path to dependencyClasses - ${params.pathsToDependencyClasses}"}
        pathsToUserClasses = params.pathsToUserClasses.split(File.pathSeparatorChar).toSet()
        pathsToDependencyClasses = params.pathsToDependencyClasses.split(File.pathSeparatorChar).toSet()
        HandlerClassesLoader.addUrls(pathsToUserClasses)
        HandlerClassesLoader.addUrls(pathsToDependencyClasses)
        kryoHelper.setKryoClassLoader(HandlerClassesLoader) // Now kryo will use our classloader when it encounters unregistered class.
        UtContext.setUtContext(UtContext(HandlerClassesLoader))
    }
    stopProcess.measureExecutionForTermination {
        logDebug { "stop request" }
        onStop()
    }
    collectCoverage.measureExecutionForTermination { params ->
        logDebug { "collect coverage request" }
        val anyClass: Class<*> = kryoHelper.readObject(params.clazz)
        logTrace { "class - ${anyClass.name}" }
        val result = (instrumentation as CoverageInstrumentation).collectCoverageInfo(anyClass)
        CollectCoverageResult(kryoHelper.writeObject(result))
    }
    computeStaticField.measureExecutionForTermination { params ->
        val fieldId = kryoHelper.readObject<FieldId>(params.fieldId)
        val result = instrumentation.getStaticField(fieldId)
        ComputeStaticFieldResult(kryoHelper.writeObject(result))
    }
}

private suspend fun initiate(lifetime: Lifetime, port: Int, pid: Int) {
    // We don't want user code to litter the standard output, so we redirect it.
    val tmpStream = PrintStream(object : OutputStream() {
        override fun write(b: Int) {}
    })
    System.setOut(tmpStream)

    Logger.set(lifetime, object : ILoggerFactory {
        override fun getLogger(category: String) = object : Logger {
            override fun isEnabled(level: LogLevel): Boolean {
                return level >= logLevel
            }

            override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
                val msg = defaultLogFormat(category, level, message, throwable)

                log(logLevel) { msg }
            }

        }
    })

    val deferred = CompletableDeferred<Unit>()
    lifetime.onTermination { deferred.complete(Unit) }
    val kryoHelper = KryoHelper(lifetime)
    logInfo { "kryo created" }

    val clientProtocol = Protocol(
        "ChildProcess",
        Serializers(),
        Identities(IdKind.Client),
        UtRdCoroutineScope.scheduler,
        SocketWire.Client(lifetime, UtRdCoroutineScope.scheduler, port),
        lifetime
    )
    val (sync, protocolModel) = obtainClientIO(lifetime, clientProtocol)

    protocolModel.setup(kryoHelper) {
        deferred.complete(Unit)
    }
    signalChildReady(pid)
    logInfo { "IO obtained" }

    val answerFromMainProcess = sync.adviseForConditionAsync(lifetime) {
        if (it == "main") {
            logTrace { "received from main" }
            measureExecutionForTermination {
                sync.fire("child")
            }
            true
        } else {
            false
        }
    }

    try {
        answerFromMainProcess.await()
        logInfo { "starting instrumenting" }
        deferred.await()
    } catch (e: Throwable) {
        logError { "Terminating process because exception occurred: ${e.stackTraceToString()}" }
    }
}