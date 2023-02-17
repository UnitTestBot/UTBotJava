package org.utbot.framework.process

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.analytics.AnalyticsConfigureUtil
import org.utbot.common.*
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
import org.utbot.summary.summarizeAll
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration.Companion.seconds

private val messageFromMainTimeoutMillis = 120.seconds
private val logger = KotlinLogging.logger {}

@Suppress("unused")
object EngineProcessMain

// use log4j2.configurationFile property to set log4j configuration
suspend fun main(args: Array<String>) = runBlocking {
    Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

    logger.info("-----------------------------------------------------------------------")
    logger.info("-------------------NEW ENGINE PROCESS STARTED--------------------------")
    logger.info("-----------------------------------------------------------------------")
    // 0 - auto port for server, should not be used here
    val port = findRdPort(args)


    ClientProtocolBuilder().withProtocolTimeout(messageFromMainTimeoutMillis).start(port) {
        AbstractSettings.setupFactory(RdSettingsContainerFactory(protocol.settingsModel))
        val kryoHelper = KryoHelper(lifetime)
        engineProcessModel.setup(kryoHelper, it, protocol)
    }
}

private lateinit var testGenerator: TestCaseGenerator
private val testSets: MutableMap<Long, List<UtMethodTestSet>> = mutableMapOf()
private val testGenerationReports: MutableList<TestsGenerationReport> = mutableListOf()
private var idCounter: Long = 0

private fun EngineProcessModel.setup(kryoHelper: KryoHelper, watchdog: IdleWatchdog, realProtocol: IProtocol) {
    val model = this
    watchdog.measureTimeForActiveCall(setupUtContext, "UtContext setup") { params ->
        UtContext.setUtContext(UtContext(URLClassLoader(params.classpathForUrlsClassloader.map {
            File(it).toURI().toURL()
        }.toTypedArray())))
    }
    watchdog.measureTimeForActiveCall(createTestGenerator, "Creating Test Generator") { params ->
        AnalyticsConfigureUtil.configureML()
        Instrumenter.adapter = RdInstrumenter(realProtocol.rdInstrumenterAdapter)
        val applicationContext: ApplicationContext = kryoHelper.readObject(params.applicationContext)

        testGenerator = TestCaseGenerator(buildDirs = params.buildDir.map { Paths.get(it) },
            classpath = params.classpath,
            dependencyPaths = params.dependencyPaths,
            jdkInfo = JdkInfo(Paths.get(params.jdkInfo.path), params.jdkInfo.version),
            applicationContext = applicationContext,
            isCanceled = {
                runBlocking {
                    model.isCancelled.startSuspending(Unit)
                }
            })
    }
    watchdog.measureTimeForActiveCall(generate, "Generating tests") { params ->
        val methods: List<ExecutableId> = kryoHelper.readObject(params.methods)
        logger.debug().measureTime({ "starting generation for ${methods.size} methods, starting with ${methods.first()}" }) {
            val mockFrameworkInstalled = params.mockInstalled
            val conflictTriggers = ConflictTriggers(kryoHelper.readObject(params.conflictTriggers))
            if (!mockFrameworkInstalled) {
                ForceMockListener.create(testGenerator, conflictTriggers, cancelJob = true)
            }
            val staticsMockingConfigured = params.staticsMockingIsConfigureda
            if (!staticsMockingConfigured) {
                ForceStaticMockListener.create(testGenerator, conflictTriggers, cancelJob = true)
            }

            val generateFlow = when (testGenerator.applicationContext) {
                is SpringApplicationContext -> defaultSpringFlow(params.generationTimeout)
                else -> testFlow {
                    generationTimeout = params.generationTimeout
                    isSymbolicEngineEnabled = params.isSymbolicEngineEnabled
                    isFuzzingEnabled = params.isFuzzingEnabled
                    fuzzingValue = params.fuzzingValue
                }
            }

            val result = testGenerator.generate(
                methods,
                MockStrategyApi.valueOf(params.mockStrategy),
                kryoHelper.readObject(params.chosenClassesToMockAlways),
                params.timeout,
                generate = generateFlow,
            )
                .summarizeAll(Paths.get(params.searchDirectory), null)
                .filterNot { it.executions.isEmpty() && it.errors.isEmpty() }

            val id = ++idCounter

            testSets[id] = result
            GenerateResult(result.size, id)
        }
    }
    watchdog.measureTimeForActiveCall(render, "Rendering tests") { params ->
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
        codeGenerator.generateAsStringWithTestReport(testSets[testSetsId]!!).let {
            testGenerationReports.add(it.testsGenerationReport)
            RenderResult(it.generatedCode, it.utilClassKind?.javaClass?.simpleName)
        }
    }
    watchdog.measureTimeForActiveCall(obtainClassId, "Obtain class id in UtContext") { canonicalName ->
        kryoHelper.writeObject(UtContext.currentContext()!!.classLoader.loadClass(canonicalName).id)
    }
    watchdog.measureTimeForActiveCall(findMethodsInClassMatchingSelected, "Find methods in Class") { params ->
        val classId = kryoHelper.readObject<ClassId>(params.classId)
        val selectedMethodDescriptions =
            params.methodDescriptions.map { MethodDescription(it.name, it.containingClass, it.parametersTypes) }
        FindMethodsInClassMatchingSelectedResult(kryoHelper.writeObject(classId.jClass.allNestedClasses.flatMap { clazz ->
            clazz.id.allMethods.mapNotNull { it.method.kotlinFunction }
                .sortedWith(compareBy { selectedMethodDescriptions.indexOf(it.methodDescription()) })
                .filter { it.methodDescription().normalized() in selectedMethodDescriptions }.map { it.executableId }
        }))
    }
    watchdog.measureTimeForActiveCall(findMethodParamNames, "Find method parameters names") { params ->
        val classId = kryoHelper.readObject<ClassId>(params.classId)
        val byMethodDescription = kryoHelper.readObject<Map<MethodDescription, List<String>>>(params.bySignature)
        FindMethodParamNamesResult(kryoHelper.writeObject(classId.jClass.allNestedClasses.flatMap { clazz -> clazz.id.allMethods.mapNotNull { it.method.kotlinFunction } }
            .mapNotNull { method -> byMethodDescription[method.methodDescription()]?.let { params -> method.executableId to params } }
            .toMap()))
    }
    watchdog.measureTimeForActiveCall(writeSarifReport, "Writing Sarif report") { params ->
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
    watchdog.measureTimeForActiveCall(generateTestReport, "Generating test report") { params ->
        val eventLogMessage = params.eventLogMessage
        val testPackageName: String? = params.testPackageName
        var hasWarnings = false
        val reports = testGenerationReports
        if (reports.isEmpty()) return@measureTimeForActiveCall GenerateTestReportResult("No tests were generated", null, true)
        val isMultiPackage = params.isMultiPackage
        val (notifyMessage, statistics) = if (reports.size == 1) {
            val report = reports.first()
            processInitialWarnings(report, params)

            val message = buildString {
                appendHtmlLine(report.toString(isShort = true))

                val classUnderTestPackageName = report.classUnderTest.java.nameOfPackage

                destinationWarningMessage(testPackageName, classUnderTestPackageName)?.let {
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
                    val classUnderTestPackageName = report.classUnderTest.java.nameOfPackage

                    hasWarnings = hasWarnings || report.hasWarnings
                    if (!isMultiPackage) {
                        val destinationWarning = destinationWarningMessage(testPackageName, classUnderTestPackageName)
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