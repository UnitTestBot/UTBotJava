package org.utbot.instrumentation.process

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import kotlinx.coroutines.*
import org.mockito.Mockito
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
import org.utbot.rd.generated.loggerModel
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.UtRdRemoteLoggerFactory
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.security.AllPermission
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
private val logger = getLogger<InstrumentedProcessMain>()
private val messageFromMainTimeout: Duration = 120.seconds

private fun closeStandardStreams() {
    // we should change out/err streams as not to spend time on user output
    // and also because rd default logging system writes some initial values to stdout, polluting it as well
    val tmpStream = PrintStream(object : OutputStream() {
        override fun write(b: Int) {}
    })
    val prevOut = System.out
    val prevError = System.err
    System.setOut(tmpStream)
    System.setErr(tmpStream)
    // stdin/stderr should be closed as not to leave hanging descriptors
    // and we cannot log any exceptions here as rd remote logging is still not configured
    // so we pass any exceptions
    silent { prevOut.close() }
    silent { prevError.close() }
}

interface DummyForMockitoWarmup {
    fun method1()
}

/**
 * Mockito initialization take ~0.5-1 sec, which forces first `invoke` request to timeout
 * it is crucial in tests as we start process just for 1-2 such requests
 */
fun warmupMockito() {
    try {
        Mockito.mock(DummyForMockitoWarmup::class.java)
    } catch (e: Throwable) {
        logger.warn { "Exception during mockito warmup: ${e.stackTraceToString()}" }
    }
}

@Suppress("unused")
object InstrumentedProcessMain

/**
 * It should be compiled into separate jar file (instrumented_process.jar) and be run with an agent (agent.jar) option.
 */
fun main(args: Array<String>) = runBlocking {
    // We don't want user code to litter the standard output, so we redirect it.
    silentlyCloseStandardStreams()

    if (!args.contains(DISABLE_SANDBOX_OPTION)) {
        permissions {
            // Enable all permissions for instrumentation.
            // SecurityKt.sandbox() is used to restrict these permissions.
            +AllPermission()
        }
    }

    val port = findRdPort(args)

    try {
        ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeout).start(port) {
            synchronizationModel.initRemoteLogging.adviseOnce(lifetime) {
                Logger.set(Lifetime.Eternal, UtRdRemoteLoggerFactory(loggerModel))
                this.protocol.scheduler.queue { warmupMockito() }
            }
            val kryoHelper = KryoHelper(lifetime)
            logger.info { "setup started" }
            AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol.settingsModel))
            instrumentedProcessModel.setup(kryoHelper, it)
            logger.info { "setup ended" }
        }
    } catch (e: Throwable) {
        logger.error { "Terminating process because exception occurred: ${e.stackTraceToString()}" }
    }
}

private lateinit var pathsToUserClasses: Set<String>
private lateinit var instrumentation: Instrumentation<*>

private var warmupDone = false

private fun InstrumentedProcessModel.setup(kryoHelper: KryoHelper, watchdog: IdleWatchdog) {
    watchdog.measureTimeForActiveCall(warmup, "Classloader warmup request") {
        if (!warmupDone) {
            HandlerClassesLoader.scanForClasses("").toList() // here we transform classes
            warmupDone = true
        } else {
            logger.info { "warmup already happened" }
        }
    }
    watchdog.measureTimeForActiveCall(invokeMethodCommand, "Invoke method request") { params ->
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
            InvokeMethodCommandResult(kryoHelper.writeObject(it))
        }) {
            throw it
        }
    }
    watchdog.measureTimeForActiveCall(setInstrumentation, "Instrumentation setup") { params ->
        logger.debug { "setInstrumentation request" }
        instrumentation = kryoHelper.readObject(params.instrumentation)
        logger.debug { "instrumentation - ${instrumentation.javaClass.name} " }
        Agent.dynamicClassTransformer.transformer = instrumentation // classTransformer is set
        Agent.dynamicClassTransformer.addUserPaths(pathsToUserClasses)
        instrumentation.init(pathsToUserClasses)
    }
    watchdog.measureTimeForActiveCall(addPaths, "User and dependency classpath setup") { params ->
        pathsToUserClasses = params.pathsToUserClasses.split(File.pathSeparatorChar).toSet()
        HandlerClassesLoader.addUrls(pathsToUserClasses)
        kryoHelper.setKryoClassLoader(HandlerClassesLoader) // Now kryo will use our classloader when it encounters unregistered class.
        UtContext.setUtContext(UtContext(HandlerClassesLoader))
    }
    watchdog.measureTimeForActiveCall(collectCoverage, "Coverage") { params ->
        val anyClass: Class<*> = kryoHelper.readObject(params.clazz)
        logger.debug { "class - ${anyClass.name}" }
        val result = (instrumentation as CoverageInstrumentation).collectCoverageInfo(anyClass)
        CollectCoverageResult(kryoHelper.writeObject(result))
    }
}