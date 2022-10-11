package org.utbot.framework.process

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.analytics.AnalyticsConfigureUtil
import org.utbot.common.AbstractSettings
import org.utbot.common.allNestedClasses
import org.utbot.common.appendHtmlLine
import org.utbot.common.nameOfPackage
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.Signature
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.process.generated.*
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.CallsSynchronizer
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.findRdPort
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.sarif.RdSourceFindingStrategyFacade
import org.utbot.sarif.SarifReport
import org.utbot.summary.summarize
import soot.SootMethod
import soot.UnitPatchingChain
import soot.util.HashChain
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.full.functions
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = KotlinLogging.logger {}

// use log4j2.configurationFile property to set log4j configuration
suspend fun main(args: Array<String>) = runBlocking {
    // 0 - auto port for server, should not be used here
    val port = findRdPort(args)

    Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(port) {
        settingsModel
        rdSourceFindingStrategy

        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol))
        val kryoHelper = KryoHelper(lifetime)
        engineProcessModel.setup(kryoHelper, it, protocol)
    }
    logger.info { "runBlocking ending" }
}.also {
    logger.info { "runBlocking ended" }
}

private lateinit var testGenerator: TestCaseGenerator
private val testSets: MutableMap<Long, List<UtMethodTestSet>> = mutableMapOf()
private val testGenerationReports: MutableList<TestsGenerationReport> = mutableListOf()
private var idCounter: Long = 0

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
        testGenerator = TestCaseGenerator(buildDirs = params.buildDir.map { Paths.get(it) },
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
            ForceMockListener.create(testGenerator, conflictTriggers, cancelJob = true)
        }
        val staticsMockingConfigured = params.staticsMockingIsConfigureda
        if (!staticsMockingConfigured) {
            ForceStaticMockListener.create(testGenerator, conflictTriggers, cancelJob = true)
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

        val id = ++idCounter

        testSets[id] = result
        GenerateResult(result.size, id)
    }
    synchronizer.measureExecutionForTermination(render) { params ->
        val testFramework = testFrameworkByName(params.testFramework)
        val staticMocking = if (params.staticsMocking.startsWith("No")) {
            NoStaticMocking
        } else {
            MockitoStaticMocking
        }
        val classId: ClassId = kryoHelper.readObject(params.classUnderTest)
        val testSetsId: Long = params.testSetsId
        val codeGenerator = CodeGenerator(
            classUnderTest = classId,
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
        codeGenerator.generateAsStringWithTestReport(testSets[testSetsId]!!)
            .let {
                testGenerationReports.add(it.testsGenerationReport)
                RenderResult(it.generatedCode, kryoHelper.writeObject(it.utilClassKind))
            }
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
                testSets[params.testSetsId]!!,
                params.generatedTestsCode,
                RdSourceFindingStrategyFacade(realProtocol.rdSourceFindingStrategy)
            ).createReport()
        )
    }
    synchronizer.measureExecutionForTermination(generateTestReport) { params ->
        val eventLogMessage = params.eventLogMessage
        val testPackageName: String? = params.testPackageName
        var hasWarnings = false
        val reports = testGenerationReports
        val isMultiPackage = params.isMultiPackage
        val (notifyMessage, statistics) = if (reports.size == 1) {
            val report = reports.first()
            processInitialWarnings(report, params)

            val message = buildString {
                appendHtmlLine(report.toString(isShort = true))

                val classUnderTestPackageName =
                    report.classUnderTest.java.nameOfPackage

                destinationWarningMessage(testPackageName, classUnderTestPackageName)
                    ?.let {
                        hasWarnings = true
                        appendHtmlLine(it)
                        appendHtmlLine()
                    }
                eventLogMessage?.let {
                    appendHtmlLine(it)
                }
            }
            hasWarnings = hasWarnings || report.hasWarnings
            Pair(message, report.detailedStatistics)
        } else {
            val accumulatedReport = reports.first()
            processInitialWarnings(accumulatedReport, params)

            val message = buildString {
                appendHtmlLine("${reports.sumBy { it.executables.size }} tests generated for ${reports.size} classes.")

                if (accumulatedReport.initialWarnings.isNotEmpty()) {
                    accumulatedReport.initialWarnings.forEach { appendHtmlLine(it()) }
                    appendHtmlLine()
                }

                // TODO maybe add statistics info here

                for (report in reports) {
                    val classUnderTestPackageName =
                        report.classUnderTest.java.nameOfPackage

                    hasWarnings = hasWarnings || report.hasWarnings
                    if (!isMultiPackage) {
                        val destinationWarning =
                            destinationWarningMessage(testPackageName, classUnderTestPackageName)
                        if (destinationWarning != null) {
                            hasWarnings = true
                            appendHtmlLine(destinationWarning)
                            appendHtmlLine()
                        }
                    }
                }
                eventLogMessage?.let {
                    appendHtmlLine(it)
                }
            }

            Pair(message, null)
        }
        GenerateTestReportResult(notifyMessage, statistics, hasWarnings)
    }
}

private fun processInitialWarnings(report: TestsGenerationReport, params: GenerateTestReportArgs) {
    val hasInitialWarnings = params.hasInitialWarnings

    if (!hasInitialWarnings) {
        return
    }

    report.apply {
        params.forceMockWarning?.let {
            initialWarnings.add { it }
        }
        params.forceStaticMockWarnings?.let {
            initialWarnings.add { it }
        }
        params.testFrameworkWarning?.let {
            initialWarnings.add { it }
        }
    }
}

private fun destinationWarningMessage(testPackageName: String?, classUnderTestPackageName: String): String? {
    return if (classUnderTestPackageName != testPackageName) {
        """
            Warning: Destination package $testPackageName does not match package of the class $classUnderTestPackageName.
            This may cause unnecessary usage of reflection for protected or package-private fields and methods access.
        """.trimIndent()
    } else {
        null
    }
}