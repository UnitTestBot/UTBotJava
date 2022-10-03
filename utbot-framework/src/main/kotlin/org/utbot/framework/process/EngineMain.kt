package org.utbot.framework.process

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.analytics.AnalyticsConfigureUtil
import org.utbot.common.AbstractSettings
import org.utbot.common.allNestedClasses
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.Signature
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.process.generated.*
import org.utbot.framework.util.ConflictTriggers
import org.utbot.framework.util.jimpleBody
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.CallsSynchronizer
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.findRdPort
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.sarif.RdSourceFindingStrategyFacade
import org.utbot.sarif.SarifRegion
import org.utbot.sarif.SarifReport
import org.utbot.summary.summarize
import soot.SootMethod
import soot.UnitPatchingChain
import soot.util.HashChain
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaType
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = KotlinLogging.logger {}

private fun KryoHelper.setup(): KryoHelper = this.apply {
    addInstantiator(soot.util.HashChain::class.java) {
        HashChain<Any>()
    }
    addInstantiator(soot.UnitPatchingChain::class.java) {
        UnitPatchingChain(HashChain())
    }
    addInstantiator(Collections.synchronizedCollection(mutableListOf<SootMethod>()).javaClass) {
        Collections.synchronizedCollection(mutableListOf<SootMethod>())
    }
    addInstantiator(Collections.synchronizedCollection(mutableListOf<Any>()).javaClass) {
        Collections.synchronizedCollection(mutableListOf<Any>())
    }
}

// use log4j2.configurationFile property to set log4j configuration
suspend fun main(args: Array<String>) = runBlocking {
    // 0 - auto port for server, should not be used here
    val port = findRdPort(args)

    Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(port) {
        settingsModel
        rdSourceFindingStrategy

        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol))
        val kryoHelper = KryoHelper(lifetime).setup()
        engineProcessModel.setup(kryoHelper, it, protocol)
    }
    logger.info { "runBlocking ending" }
}.also {
    logger.info { "runBlocking ended" }
}

private lateinit var testGenerator: TestCaseGenerator
private lateinit var testSets: List<UtMethodTestSet>

private fun EngineProcessModel.setup(
    kryoHelper: KryoHelper, synchronizer: CallsSynchronizer, realProtocol: IProtocol
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
                    model.isCancelled.startSuspending(Unit)
                }
            })
    }
    synchronizer.measureExecutionForTermination(generate) { params ->
        val mockFrameworkInstalled = params.mockInstalled
        val conflictTriggers = ConflictTriggers(kryoHelper.readObject(params.conflictTriggers))
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
            })
            .map { it.summarize(Paths.get(params.searchDirectory)) }
            .filterNot { it.executions.isEmpty() && it.errors.isEmpty() }
            .apply {
                testSets = this
            }
            .map {
                // kryo cannot serialize structures such as JimpleBody or Api.Step.stmt
                // because soot entities use classes and collections with complex initialization logic
                // which kryo cannot guess.
                // so in the idea process such entities will be either null or empty, operating on them supported only
                // in engine process.
                // jimpleBody can be obtained from executionId, logic for path and fullpath should be discussed
                it.copy(jimpleBody = null, executions = it.executions.map { execution ->
                    if (execution is UtSymbolicExecution) execution.apply {
                        path = mutableListOf()
                        fullPath = mutableListOf()
                    }
                    else execution
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
                codeGenerator.generateAsStringWithTestReport(kryoHelper.readObject<List<UtMethodTestSet>>(
                    params.testSets
                ).map { it.copy(jimpleBody = jimpleBody(it.method)) })
                    // currently due to moving engine out of idea process we cannot show test generation report
                    // because it contains CgElements, which kryo cannot deserialize because they use complex collections
                    // potentially it is possible to implement a lot of additional kryo serializers for that collections
                    .copy(testsGenerationReport = null)
            )
        )
    }
    synchronizer.measureExecutionForTermination(stopProcess) { synchronizer.stopProtocol() }
    synchronizer.measureExecutionForTermination(obtainClassId) { canonicalName ->
        kryoHelper.writeObject(UtContext.currentContext()!!.classLoader.loadClass(canonicalName).id)
    }
    synchronizer.measureExecutionForTermination(findMethodsInClassMatchingSelected) { params ->
        val classId = kryoHelper.readObject<ClassId>(params.classId)
        val selectedSignatures = params.signatures.map { Signature(it.name, it.parametersTypes) }
        FindMethodsInClassMatchingSelectedResult(kryoHelper.writeObject(classId.jClass.kotlin.allNestedClasses.flatMap { clazz ->
            clazz.functions.sortedWith(compareBy { selectedSignatures.indexOf(it.signature()) })
                .filter { it.signature().normalized() in selectedSignatures }
                .map { it.executableId }
        }))
    }
    synchronizer.measureExecutionForTermination(findMethodParamNames) { params ->
        val classId = kryoHelper.readObject<ClassId>(params.classId)
        val bySignature = kryoHelper.readObject<Map<Signature, List<String>>>(params.bySignature)
        FindMethodParamNamesResult(kryoHelper.writeObject(
            classId.jClass.kotlin.allNestedClasses.flatMap { it.functions }
                .mapNotNull { method -> bySignature[method.signature()]?.let { params -> method.executableId to params } }
                .toMap()
        ))
    }
    synchronizer.measureExecutionForTermination(writeSarifReport) { params ->
        val reportFilePath = Paths.get(params.reportFilePath)
        reportFilePath.toFile().writeText(
            SarifReport(
                testSets,
                params.generatedTestsCode,
                RdSourceFindingStrategyFacade(realProtocol.rdSourceFindingStrategy)
            ).createReport()
        )
    }
}