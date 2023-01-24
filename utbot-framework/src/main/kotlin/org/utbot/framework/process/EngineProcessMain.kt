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
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.NoStaticMocking
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.testFrameworkByName
import org.utbot.framework.codegen.reports.TestsGenerationReport
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.MethodDescription
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.process.generated.*
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.IdleWatchdog
import org.utbot.rd.ClientProtocolBuilder
import org.utbot.rd.RdSettingsContainerFactory
import org.utbot.rd.findRdPort
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.sarif.RdSourceFindingStrategyFacade
import org.utbot.sarif.SarifReport
import org.utbot.summary.summarize
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = KotlinLogging.logger {}

// use log4j2.configurationFile property to set log4j configuration
suspend fun main(args: Array<String>) = runBlocking {
    // 0 - auto port for server, should not be used here
    val port = findRdPort(args)

    Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(port) {
        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol.settingsModel))
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

private fun EngineProcessModel.setup(kryoHelper: KryoHelper, watchdog: IdleWatchdog, realProtocol: IProtocol) {
    val model = this
    watchdog.wrapActiveCall(setupUtContext) { params ->
        UtContext.setUtContext(UtContext(URLClassLoader(params.classpathForUrlsClassloader.map {
            File(it).toURI().toURL()
        }.toTypedArray())))
    }
    watchdog.wrapActiveCall(createTestGenerator) { params ->
        AnalyticsConfigureUtil.configureML()
        Instrumenter.adapter = RdInstrumenter(realProtocol.rdInstrumenterAdapter)
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
    watchdog.wrapActiveCall(generate) { params ->
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
                isGreyBoxFuzzingEnabled = params.isGreyBoxFuzzingEnabled
            })
            .apply { logger.info("generation ended, starting summarization, result size: ${this.size}") }
            .map { it.summarize(Paths.get(params.searchDirectory), sourceFile = null) }
            .apply { logger.info("summarization ended") }
            .filterNot { it.executions.isEmpty() && it.errors.isEmpty() }

        val id = ++idCounter

        testSets[id] = result
        GenerateResult(result.size, id)
    }
    watchdog.wrapActiveCall(render) { params ->
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
                RenderResult(it.generatedCode, it.utilClassKind?.javaClass?.simpleName)
            }
    }
    watchdog.wrapActiveCall(stopProcess) { watchdog.stopProtocol() }
    watchdog.wrapActiveCall(obtainClassId) { canonicalName ->
        kryoHelper.writeObject(UtContext.currentContext()!!.classLoader.loadClass(canonicalName).id)
    }
    watchdog.wrapActiveCall(findMethodsInClassMatchingSelected) { params ->
        val classId = kryoHelper.readObject<ClassId>(params.classId)
        val selectedMethodDescriptions =
            params.methodDescriptions.map { MethodDescription(it.name, it.containingClass, it.parametersTypes) }
        FindMethodsInClassMatchingSelectedResult(kryoHelper.writeObject(classId.jClass.allNestedClasses.flatMap { clazz ->
            clazz.id.allMethods.mapNotNull { it.method.kotlinFunction }
                .sortedWith(compareBy { selectedMethodDescriptions.indexOf(it.methodDescription()) })
                .filter { it.methodDescription().normalized() in selectedMethodDescriptions }
                .map { it.executableId }
        }))
    }
    watchdog.wrapActiveCall(findMethodParamNames) { params ->
        val classId = kryoHelper.readObject<ClassId>(params.classId)
        val byMethodDescription = kryoHelper.readObject<Map<MethodDescription, List<String>>>(params.bySignature)
        FindMethodParamNamesResult(kryoHelper.writeObject(
            classId.jClass.allNestedClasses.flatMap { clazz -> clazz.id.allMethods.mapNotNull { it.method.kotlinFunction } }
                .mapNotNull { method -> byMethodDescription[method.methodDescription()]?.let { params -> method.executableId to params } }
                .toMap()
        ))
    }
    watchdog.wrapActiveCall(writeSarifReport) { params ->
        val reportFilePath = Paths.get(params.reportFilePath)
        reportFilePath.parent.toFile().mkdirs()
        val sarifReport = SarifReport(
            testSets[params.testSetsId]!!,
            params.generatedTestsCode,
            RdSourceFindingStrategyFacade(params.testSetsId, realProtocol.rdSourceFindingStrategy)
        ).createReport().toJson()
        reportFilePath.toFile().writeText(sarifReport)
        sarifReport
    }
    watchdog.wrapActiveCall(generateTestReport) { params ->
        val eventLogMessage = params.eventLogMessage
        val testPackageName: String? = params.testPackageName
        var hasWarnings = false
        val reports = testGenerationReports
        if (reports.isEmpty()) return@wrapActiveCall GenerateTestReportResult("No tests were generated", null, true)
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
                appendHtmlLine("${reports.sumOf { it.countTestMethods() }} tests generated for ${reports.size} classes.")

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