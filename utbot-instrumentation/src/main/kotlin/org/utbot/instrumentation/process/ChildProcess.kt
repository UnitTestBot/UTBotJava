package org.utbot.instrumentation.process

import com.jetbrains.rd.framework.util.launchChild
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.defaultLogFormat
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.plusAssign
import kotlinx.coroutines.*
import org.utbot.common.getCurrentProcessId
import org.utbot.common.scanForClasses
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.agent.Agent
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.rd.childCreatedFileName
import org.utbot.instrumentation.rd.obtainClientIO
import org.utbot.instrumentation.rd.processSyncDirectory
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.instrumentation.util.Protocol
import org.utbot.instrumentation.util.UnexpectedCommand
import org.utbot.rd.UtRdUtil
import org.utbot.rd.UtSingleThreadScheduler
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * We use this ClassLoader to separate user's classes and our dependency classes.
 * Our classes won't be instrumented.
 */
internal object HandlerClassesLoader : URLClassLoader(emptyArray()) {
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

typealias ChildProcessLogLevel = LogLevel

private val logLevel = ChildProcessLogLevel.Info

// Logging
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
private fun log(level: ChildProcessLogLevel, any: () -> Any?) {
    if (level < logLevel)
        return

    System.err.println(LocalDateTime.now().format(dateFormatter) + " | ${any()}")
}

private fun logError(any: () -> Any?) {
    log(ChildProcessLogLevel.Error, any)
}

private fun logInfo(any: () -> Any?) {
    log(ChildProcessLogLevel.Info, any)
}

private fun logTrace(any: () -> Any?) {
    log(ChildProcessLogLevel.Trace, any)
}

private val readStart = AtomicLong(0)
private val readEnd = AtomicLong(0)
private val messageFromMainTimeoutMillis = 120 * 1000

/**
 * It should be compiled into separate jar file (child_process.jar) and be run with an agent (agent.jar) option.
 */
fun main(args: Array<String>) {
    // 0 - auto port for server, should not be used here
    val port = args.find { it.startsWith(serverPortProcessArgumentTag) }
        ?.run { split("=").last().toInt().coerceIn(1..65535) }
        ?: throw IllegalArgumentException("No port provided")

    val pid = getCurrentProcessId()
    val def = LifetimeDefinition()

    GlobalScope.launchChild(Lifetime.Eternal, Dispatchers.Unconfined) {
        while (true) {
            val now = System.currentTimeMillis()
            val start = readStart.get()
            val end = readEnd.get()

            if (start <= end) { // process is doing something
                delay(1000)
            } else { // process is waiting for answer
                if (now - start > messageFromMainTimeoutMillis) {
                    logInfo { "terminating lifetime" }
                    def.terminate()
                    break
                } else {
                    delay(1000)
                }
            }
        }
    }

    def.usingNested { lifetime ->
        lifetime += { logInfo { "lifetime terminated" } }
        initiate(lifetime, port, pid.toInt())
    }
}

private fun initiate(lifetime: Lifetime, port: Int, pid: Int) = lifetime.bracketIfAlive({
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

    val clientProtocol = UtRdUtil.createUtClientProtocol(lifetime, port, UtSingleThreadScheduler { logInfo(it) })
    logInfo {
        "hearthbeatAlive - ${clientProtocol.wire.heartbeatAlive.value}, connected - ${
            clientProtocol.wire.connected.value
        }"
    }

    val (mainToProcess, processToMain, sync) = obtainClientIO(lifetime, clientProtocol, pid)
    logInfo { "IO obtained" }

    val kryoHelper =
        KryoHelper(lifetime, mainToProcess, processToMain)
        { logTrace(it) }
    logInfo { "kryo created" }

    val latch = CountDownLatch(1)
    sync.advise(lifetime) {
        if (it == "main") {
            sync.fire("child")
            latch.countDown()
        }
    }

    if (latch.await(messageFromMainTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)) {
        logInfo { "starting instrumenting" }
        try {
            startInstrumenting(kryoHelper)
        } catch (e: Throwable) {
            logError { "Terminating process because exception occured: ${e.stackTraceToString()}" }
        }
    }
}) {
    val syncFile = File(processSyncDirectory, childCreatedFileName(pid))

    if (syncFile.exists()) {
        logInfo { "sync file existed" }
        syncFile.delete()
    }
}

private fun startInstrumenting(kryoHelper: KryoHelper) {
    val classPaths = readClasspath(kryoHelper) ?: return
    logTrace { "classpaths read $classPaths" }

    val pathsToUserClasses = classPaths.pathsToUserClasses.split(File.pathSeparatorChar).toSet()
    val pathsToDependencyClasses = classPaths.pathsToDependencyClasses.split(File.pathSeparatorChar).toSet()
    HandlerClassesLoader.addUrls(pathsToUserClasses)
    HandlerClassesLoader.addUrls(pathsToDependencyClasses)
    kryoHelper.setKryoClassLoader(HandlerClassesLoader) // Now kryo will use our classloader when it encounters unregistered class.

    logTrace { "User classes:" + pathsToUserClasses.joinToString() }

    UtContext.setUtContext(UtContext(HandlerClassesLoader)).use {
        getInstrumentation(kryoHelper)?.let { instrumentation ->
            Agent.dynamicClassTransformer.transformer = instrumentation // classTransformer is set
            Agent.dynamicClassTransformer.addUserPaths(pathsToUserClasses)
            instrumentation.init(pathsToUserClasses)
            loop(kryoHelper, instrumentation)
        }
    }
}

private fun send(kryoHelper: KryoHelper, cmdId: Long, cmd: Protocol.Command) {
    try {
        kryoHelper.writeCommand(cmdId, cmd)
        logInfo { "Send id << $cmdId" }
        logTrace { "Send message for $cmdId << $cmd" }
    } catch (e: Exception) {
        logError { "Failed to serialize << $cmdId with exception: ${e.stackTraceToString()}" }
        logInfo { "Writing it to kryo..." }
        kryoHelper.writeCommand(cmdId, Protocol.ExceptionInChildProcess(e))
        logInfo { "Successfuly wrote." }
    }
}

private fun read(kryoHelper: KryoHelper): KryoHelper.ReceivedCommand {
    readStart.set(System.currentTimeMillis())
    val cmd = kryoHelper.readCommand()
    readEnd.set(System.currentTimeMillis())
    logInfo { "Received :> ${cmd.cmdId}" }
    logTrace { "Received for id ${cmd.cmdId} :> ${cmd.command}" }
    return cmd
}

/**
 * Main loop. Processes incoming commands.
 */
private fun loop(kryoHelper: KryoHelper, instrumentation: Instrumentation<*>) {
    logInfo { "starting looping" }
    while (true) {
        val (id, cmd) = try {
            read(kryoHelper)
        } catch (e: CancellationException) {
            return
        } catch (e: Exception) {
            logError { "error while trying read: $e" }
            kryoHelper.discard()
            continue
        }

        logInfo { "read cmdId: $id" }
        logTrace { "read cmd for id $id: $cmd" }

        when (cmd) {
            is Protocol.WarmupCommand -> {
                val time = measureTimeMillis {
                    HandlerClassesLoader.scanForClasses("").toList() // here we transform classes
                }
                logInfo { "warmup finished in $time ms" }
            }

            is Protocol.InvokeMethodCommand -> {
                val resultCmd = try {
                    val clazz = HandlerClassesLoader.loadClass(cmd.className)
                    val res = instrumentation.invoke(
                        clazz,
                        cmd.signature,
                        cmd.arguments,
                        cmd.parameters
                    )
                    Protocol.InvocationResultCommand(res)
                } catch (e: Throwable) {
                    logInfo { "error in invokeMethod: ${e.stackTraceToString()}" }
                    Protocol.ExceptionInChildProcess(e)
                }

                send(kryoHelper, id, resultCmd)
                logInfo { "sent cmd: $resultCmd" }
            }

            is Protocol.StopProcessCommand -> {
                break
            }

            is Protocol.InstrumentationCommand -> {
                val result = instrumentation.handle(cmd)
                result?.let {
                    send(kryoHelper, id, it)
                }
            }

            else -> {
                logError { "unexpected command: $cmd" }
                send(kryoHelper, id, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            }
        }

        logInfo { "cmd $id executed" }
    }
}

/**
 * Retrieves the actual instrumentation. It is passed from the main process during
 * [org.utbot.instrumentation.ConcreteExecutor] instantiation.
 */
private fun getInstrumentation(kryoHelper: KryoHelper): Instrumentation<*>? {
    logInfo { "reading instrumentation" }
    val (id, cmd) = read(kryoHelper)
    return when (cmd) {
        is Protocol.SetInstrumentationCommand<*> -> {
            send(kryoHelper, id, Protocol.OperationCompleted())
            cmd.instrumentation
        }

        is Protocol.StopProcessCommand -> {
            null
        }

        else -> {
            send(kryoHelper, id, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            error("No instrumentation!")
        }
    }
}

private fun readClasspath(kryoHelper: KryoHelper): Protocol.AddPathsCommand? {
    logInfo { "reading classpath" }
    val (id, cmd) = read(kryoHelper)
    return when (cmd) {
        is Protocol.AddPathsCommand -> {
            send(kryoHelper, id, Protocol.OperationCompleted())
            cmd
        }

        is Protocol.StopProcessCommand -> {
            null
        }

        else -> {
            send(kryoHelper, id, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            error("No classpath!")
        }
    }
}