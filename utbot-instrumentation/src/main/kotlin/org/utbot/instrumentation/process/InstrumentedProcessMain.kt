package org.utbot.instrumentation.process

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.*
import org.utbot.common.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.agent.Agent
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.process.generated.CollectCoverageResult
import org.utbot.instrumentation.process.generated.InstrumentedProcessModel
import org.utbot.instrumentation.process.generated.InvokeMethodCommandResult
import org.utbot.instrumentation.process.generated.instrumentedProcessModel
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.IdleWatchdog
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.RdSettingsContainerFactory
import org.utbot.rd.findRdPort
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.loggers.UtRdConsoleLoggerFactory
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.security.AllPermission
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

/**
 * Command-line option to disable the sandbox
 */
const val DISABLE_SANDBOX_OPTION = "--disable-sandbox"
const val ENABLE_LOGS_OPTION = "--enable-logs"
private val logger = getLogger("InstrumentedProcess")
private val messageFromMainTimeout: Duration = 120.seconds

fun logLevelArgument(level: LogLevel): String {
    return "$ENABLE_LOGS_OPTION=$level"
}

private fun findLogLevel(args: Array<String>): LogLevel {
    val logArgument = args.find{ it.contains(ENABLE_LOGS_OPTION) } ?: return LogLevel.Fatal

    return enumValueOf(logArgument.split("=").last())
}

/**
 * It should be compiled into separate jar file (instrumented_process.jar) and be run with an agent (agent.jar) option.
 */
fun main(args: Array<String>) = runBlocking {
    // We don't want user code to litter the standard output, so we redirect it.
    val tmpStream = PrintStream(object : OutputStream() {
        override fun write(b: Int) {}
    })

    System.setOut(tmpStream)

    if (!args.contains(DISABLE_SANDBOX_OPTION)) {
        permissions {
            // Enable all permissions for instrumentation.
            // SecurityKt.sandbox() is used to restrict these permissions.
            +AllPermission()
        }
    }

    val logLevel: LogLevel = findLogLevel(args)
    Logger.set(Lifetime.Eternal, UtRdConsoleLoggerFactory(logLevel, System.err))

    val port = findRdPort(args)

    try {
        ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeout).start(port) {
            val kryoHelper = KryoHelper(lifetime)
            logger.info { "setup started" }
            AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol.settingsModel))
            instrumentedProcessModel.setup(kryoHelper, it)
            logger.info { "setup ended" }
        }
    } catch (e: Throwable) {
        logger.error { "Terminating process because exception occurred: ${e.stackTraceToString()}" }
    }
    logger.info { "runBlocking ending" }
}.also {
    logger.info { "runBlocking ended" }
}

private lateinit var pathsToUserClasses: Set<String>
private lateinit var instrumentation: Instrumentation<*>

private var warmupDone = false

private fun InstrumentedProcessModel.setup(kryoHelper: KryoHelper, watchdog: IdleWatchdog) {
    watchdog.wrapActiveCall(warmup) {
        logger.info { "received warmup request" }
        if (!warmupDone) {
            val time = measureTimeMillis {
                HandlerClassesLoader.scanForClasses("").toList() // here we transform classes
            }
            logger.info { "warmup finished in $time ms" }
            warmupDone = true
        } else {
            logger.info { "warmup already happened" }
        }
    }
    watchdog.wrapActiveCall(invokeMethodCommand) { params ->
        logger.debug { "received invokeMethod request: ${params.classname}, ${params.signature}" }
        val clazz = HandlerClassesLoader.loadClass(params.classname)
        val res = kotlin.runCatching {
            instrumentation.invoke(
                clazz,
                params.signature,
                kryoHelper.readObject(params.arguments),
                kryoHelper.readObject(params.parameters)
            )
        }
        res.fold({
            logger.debug { "invokeMethod success" }
            InvokeMethodCommandResult(kryoHelper.writeObject(it))
        }) {
            logger.debug { "invokeMethod failure" }
            logger.error(it)
            throw it
        }
    }
    watchdog.wrapActiveCall(setInstrumentation) { params ->
        logger.debug { "setInstrumentation request" }
        instrumentation = kryoHelper.readObject(params.instrumentation)
        logger.trace { "instrumentation - ${instrumentation.javaClass.name} " }
        Agent.dynamicClassTransformer.transformer = instrumentation // classTransformer is set
        Agent.dynamicClassTransformer.addUserPaths(pathsToUserClasses)
        instrumentation.init(pathsToUserClasses)
    }
    watchdog.wrapActiveCall(addPaths) { params ->
        logger.debug { "addPaths request" }
        pathsToUserClasses = params.pathsToUserClasses.split(File.pathSeparatorChar).toSet()
        HandlerClassesLoader.addUrls(pathsToUserClasses)
        kryoHelper.setKryoClassLoader(HandlerClassesLoader) // Now kryo will use our classloader when it encounters unregistered class.
        UtContext.setUtContext(UtContext(HandlerClassesLoader))
    }
    watchdog.wrapActiveCall(stopProcess) {
        logger.debug { "stop request" }
        watchdog.stopProtocol()
    }
    watchdog.wrapActiveCall(collectCoverage) { params ->
        logger.debug { "collect coverage request" }
        val anyClass: Class<*> = kryoHelper.readObject(params.clazz)
        logger.debug { "class - ${anyClass.name}" }
        val result = (instrumentation as CoverageInstrumentation).collectCoverageInfo(anyClass)
        CollectCoverageResult(kryoHelper.writeObject(result))
    }
}