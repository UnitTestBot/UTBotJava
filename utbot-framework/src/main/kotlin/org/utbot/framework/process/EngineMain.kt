package org.utbot.framework.process

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.analytics.AnalyticsConfigureUtil
import org.utbot.common.AbstractSettings
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.process.generated.*
import org.utbot.framework.util.ConflictTriggers
import org.utbot.framework.util.jimpleBody
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.CallsSynchronizer
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.awaitTermination
import org.utbot.rd.findRdPort
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.summary.summarize
import soot.SootMethod
import soot.UnitPatchingChain
import soot.util.HashChain
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = KotlinLogging.logger {}

// use log4j2.configurationFile property to set log4j configuration
suspend fun main(args: Array<String>) = runBlocking {
    // 0 - auto port for server, should not be used here
    val port = findRdPort(args)
    val ldef = LifetimeDefinition()
    val kryoHelper = KryoHelper(ldef)

    kryoHelper.addInstantiator(soot.util.HashChain::class.java) {
        HashChain<Any>()
    }
    kryoHelper.addInstantiator(soot.UnitPatchingChain::class.java) {
        UnitPatchingChain(HashChain())
    }
    kryoHelper.addInstantiator(Collections.synchronizedCollection(mutableListOf<SootMethod>()).javaClass) {
        Collections.synchronizedCollection(mutableListOf<SootMethod>())
    }
    kryoHelper.addInstantiator(Collections.synchronizedCollection(mutableListOf<Any>()).javaClass) {
        Collections.synchronizedCollection(mutableListOf<Any>())
    }

    Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(ldef, port) {
        settingsModel
        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol))
        engineProcessModel.setup(ldef, kryoHelper, it) {
            ldef.terminate()
        }
    }

    ldef.awaitTermination()
}

private lateinit var testGenerator: TestCaseGenerator

private fun EngineProcessModel.setup(
    lifetime: Lifetime,
    kryoHelper: KryoHelper,
    synchronizer: CallsSynchronizer,
    onStop: () -> Unit
) {
    val model = this
    synchronizer.measureExecutionForTermination(setupUtContext) { params ->
        UtContext.setUtContext(UtContext(URLClassLoader(params.classpathForUrlsClassloader.map {
            File(it).toURI().toURL()
        }.toTypedArray())))
    }
    synchronizer.measureExecutionForTermination(createTestGenerator) { params ->
        AnalyticsConfigureUtil.configureML()
        testGenerator = TestCaseGenerator(buildDir = Paths.get(params.buildDir),
            classpath = params.classpath,
            dependencyPaths = params.dependencyPaths,
            jdkInfo = JdkInfo(Paths.get(params.jdkInfo.path), params.jdkInfo.version),
            isCanceled = {
                runBlocking {
                    model.isCancelled.startSuspending(lifetime, Unit)
                }
            })
    }
    synchronizer.measureExecutionForTermination(generate) { params ->
        val mockFrameworkInstalled = params.mockInstalled
        val conflictTriggers =
            ConflictTriggers(kryoHelper.readObject(params.conflictTriggers))
        if (!mockFrameworkInstalled) {
            ForceMockListener.create(testGenerator, conflictTriggers)
        }
        val staticsMockingConfigured = params.staticsMockingIsConfigureda
        if (!staticsMockingConfigured) {
            ForceStaticMockListener.create(testGenerator, conflictTriggers)
        }
        val result = testGenerator.generate(kryoHelper.readObject(params.methods),
            MockStrategyApi.valueOf(params.mockStrategy),
            kryoHelper.readObject(params.chosenClassesToMockAlways),
            params.timeout,
            generate = testFlow {
                generationTimeout = params.generationTimeout
                isSymbolicEngineEnabled = params.isSymbolicEngineEnabled
                isFuzzingEnabled = params.isFuzzingEnabled
                fuzzingValue = params.fuzzingValue
            }).map { it.summarize(Paths.get(params.searchDirectory)) }
            .filterNot { it.executions.isEmpty() && it.errors.isEmpty() }.map {
                it.copy(jimpleBody = null, executions = it.executions.map { execution ->
                    if (execution is UtSymbolicExecution) execution.apply {
                        path = mutableListOf()
                        fullPath = mutableListOf()
                    }
                    else
                        execution
                })
            }


        GenerateResult(kryoHelper.writeObject(result))
    }
    synchronizer.measureExecutionForTermination(render) { params ->
        val testFramework = testFrameworkByName(params.testFramework)
        val staticMocking = if (params.staticsMocking.startsWith("No")) {
            NoStaticMocking
        } else {
            MockitoStaticMocking
        }
        val codeGenerator = CodeGenerator(
            classUnderTest = kryoHelper.readObject(params.classUnderTest),
            generateUtilClassFile = params.generateUtilClassFile,
            paramNames = kryoHelper.readObject(params.paramNames),
            testFramework = testFramework,
            mockFramework = MockFramework.valueOf(params.mockFramework),
            codegenLanguage = CodegenLanguage.valueOf(params.codegenLanguage),
            parameterizedTestSource = ParametrizedTestSource.valueOf(params.parameterizedTestSource),
            staticsMocking = staticMocking,
            forceStaticMocking = kryoHelper.readObject(params.forceStaticMocking),
            generateWarningsForStaticMocking = params.generateWarningsForStaticMocking,
            runtimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.valueOf(params.runtimeExceptionTestsBehaviour),
            hangingTestsTimeout = HangingTestsTimeout(params.hangingTestsTimeout),
            enableTestsTimeout = params.enableTestsTimeout,
            testClassPackageName = params.testClassPackageName
        )
        RenderResult(
            kryoHelper.writeObject(
                codeGenerator.generateAsStringWithTestReport(
                    kryoHelper.readObject<List<UtMethodTestSet>>(
                        params.testSets
                    ).map { it.copy(jimpleBody = jimpleBody(it.method)) }).copy(testsGenerationReport = null)
            )
        )
    }
    synchronizer.measureExecutionForTermination(stopProcess) { onStop() }
}
