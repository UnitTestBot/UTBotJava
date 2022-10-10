package org.utbot.intellij.plugin.process

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.utbot.common.*
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.OpenModulesContainer
import org.utbot.framework.process.generated.*
import org.utbot.framework.process.generated.Signature
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.util.signature
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.rdPortArgument
import org.utbot.rd.startUtProcessWithRdServer
import org.utbot.sarif.SourceFindingStrategy
import java.io.File
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val engineProcessLogConfigurations = utBotTempDirectory.toFile().resolve("rdEngineProcessLogConfigurations")
private val logger = KotlinLogging.logger {}
private val engineProcessLogDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogs")

data class RdTestGenerationResult(val notEmptyCases: Int, val testSetsId: Long)

class EngineProcess(parent: Lifetime, val project: Project) {
    private val ldef = parent.createNested()
    private val id = Random.nextLong()
    private var count = 0
    private var configPath: Path? = null

    private fun getOrCreateLogConfig(): String {
        var realPath = configPath
        if (realPath == null) {
            engineProcessLogConfigurations.mkdirs()
            configPath = File.createTempFile("epl", ".xml", engineProcessLogConfigurations).apply {
                val onMatch = if (UtSettings.logConcreteExecutionErrors) "NEUTRAL" else "DENY"
                writeText(
                    """<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <ThresholdFilter level="${UtSettings.engineProcessLogLevel.name.uppercase()}"  onMatch="$onMatch"   onMismatch="DENY"/>
            <PatternLayout pattern="%d{HH:mm:ss.SSS} | %-5level | %c{1} | %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="${UtSettings.engineProcessLogLevel.name.lowercase()}">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>"""
                )
            }.toPath()
            realPath = configPath
            logger.info("log configuration path - ${realPath!!.pathString}")
        }
        return realPath.pathString
    }

    private fun debugArgument(): String {
        return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005".takeIf { Settings.runIdeaProcessWithDebug }
            ?: ""
    }

    private val kryoHelper = KryoHelper(ldef)

    private suspend fun engineModel(): EngineProcessModel {
        ldef.throwIfNotAlive()
        return lock.withLock {
            var proc = current

            if (proc == null) {
                proc = startUtProcessWithRdServer(ldef) { port ->
                    val current = JdkInfoDefaultProvider().info
                    val required = JdkInfoService.jdkInfoProvider.info
                    val java =
                        JdkInfoService.jdkInfoProvider.info.path.resolve("bin${File.separatorChar}${osSpecificJavaExecutable()}").toString()
                    val cp = (this.javaClass.classLoader as PluginClassLoader).classPath.baseUrls.joinToString(
                        separator = if (isWindows) ";" else ":",
                        prefix = "\"",
                        postfix = "\""
                    )
                    val classname = "org.utbot.framework.process.EngineMainKt"
                    val javaVersionSpecificArguments = OpenModulesContainer.javaVersionSpecificArguments
                    val directory = WorkingDirService.provide().toFile()
                    val log4j2ConfigFile = "-Dlog4j2.configurationFile=${getOrCreateLogConfig()}"
                    val debugArg = debugArgument()
                    logger.info { "java - $java\nclasspath - $cp\nport - $port" }
                    val cmd = mutableListOf<String>(java, "-ea")
                    if (javaVersionSpecificArguments.isNotEmpty()) {
                        cmd.addAll(javaVersionSpecificArguments)
                    }
                    if (debugArg.isNotEmpty()) {
                        cmd.add(debugArg)
                    }
                    cmd.add(log4j2ConfigFile)
                    cmd.add("-cp")
                    cmd.add(cp)
                    cmd.add(classname)
                    cmd.add(rdPortArgument(port))
                    ProcessBuilder(cmd).directory(directory).apply {
                        if (UtSettings.logConcreteExecutionErrors) {
                            engineProcessLogDirectory.mkdirs()
                            val logFile = File(engineProcessLogDirectory, "${id}-${count++}.log")
                            logger.info { "logFile - ${logFile.canonicalPath}" }
                            redirectOutput(logFile)
                            redirectError(logFile)
                        }
                    }.start().apply {
                        logger.debug { "Engine process started with PID = ${this.getPid}" }
                    }
                }.initModels {
                    engineProcessModel
                    rdSourceFindingStrategy
                    settingsModel.settingFor.set { params ->
                        SettingForResult(AbstractSettings.allSettings[params.key]?.let { settings: AbstractSettings ->
                            val members: Collection<KProperty1<AbstractSettings, *>> =
                                settings.javaClass.kotlin.memberProperties
                            val names: List<KProperty1<AbstractSettings, *>> =
                                members.filter { it.name == params.propertyName }
                            val sing: KProperty1<AbstractSettings, *> = names.single()
                            val result = sing.get(settings)
                            logger.trace { "request for settings ${params.key}:${params.propertyName} - $result" }
                            result.toString()
                        })
                    }
                }.awaitSignal()
                current = proc
            }

            proc.protocol.engineProcessModel
        }
    }

    private val lock = Mutex()
    private var current: ProcessWithRdServer? = null

    fun setupUtContext(classpathForUrlsClassloader: List<String>) = runBlocking {
        engineModel().setupUtContext.startSuspending(ldef, SetupContextParams(classpathForUrlsClassloader))
    }

    // suppose that only 1 simultaneous test generator process can be executed in idea
    // so every time test generator is created - we just overwrite previous
    fun createTestGenerator(
        buildDir: List<String>,
        classPath: String?,
        dependencyPaths: String,
        jdkInfo: JdkInfo,
        isCancelled: (Unit) -> Boolean
    ) = runBlocking {
        engineModel().isCancelled.set(handler = isCancelled)
        engineModel().createTestGenerator.startSuspending(
            ldef,
            TestGeneratorParams(buildDir.toTypedArray(), classPath, dependencyPaths, JdkInfo(jdkInfo.path.pathString, jdkInfo.version))
        )
    }

    fun obtainClassId(canonicalName: String): ClassId = runBlocking {
        kryoHelper.readObject(engineModel().obtainClassId.startSuspending(canonicalName))
    }

    fun findMethodsInClassMatchingSelected(clazzId: ClassId, srcMethods: List<MemberInfo>): List<ExecutableId> =
        runBlocking {
            val srcSignatures = srcMethods.map { it.signature() }
            val rdSignatures = srcSignatures.map {
                Signature(it.name, it.parameterTypes)
            }
            kryoHelper.readObject(
                engineModel().findMethodsInClassMatchingSelected.startSuspending(
                    FindMethodsInClassMatchingSelectedArguments(kryoHelper.writeObject(clazzId), rdSignatures)
                ).executableIds
            )
        }

    fun findMethodParamNames(classId: ClassId, methods: List<MemberInfo>): Map<ExecutableId, List<String>> =
        runBlocking {
            val bySignature = methods.associate { it.signature() to it.paramNames() }
            kryoHelper.readObject(
                engineModel().findMethodParamNames.startSuspending(
                    FindMethodParamNamesArguments(
                        kryoHelper.writeObject(
                            classId
                        ), kryoHelper.writeObject(bySignature)
                    )
                ).paramNames
            )
        }

    private fun MemberInfo.paramNames(): List<String> =
        (this.member as PsiMethod).parameterList.parameters.map { it.name }

    fun generate(
        mockInstalled: Boolean,
        staticsMockingIsConfigured: Boolean,
        conflictTriggers: ConflictTriggers,
        methods: List<ExecutableId>,
        mockStrategyApi: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId>,
        timeout: Long,
        generationTimeout: Long,
        isSymbolicEngineEnabled: Boolean,
        isFuzzingEnabled: Boolean,
        fuzzingValue: Double,
        searchDirectory: String
    ): RdTestGenerationResult = runBlocking {
        val result = engineModel().generate.startSuspending(
            ldef,
            GenerateParams(
                mockInstalled,
                staticsMockingIsConfigured,
                kryoHelper.writeObject(conflictTriggers.toMutableMap()),
                kryoHelper.writeObject(methods),
                mockStrategyApi.name,
                kryoHelper.writeObject(chosenClassesToMockAlways),
                timeout,
                generationTimeout,
                isSymbolicEngineEnabled,
                isFuzzingEnabled,
                fuzzingValue,
                searchDirectory
            )
        )

        return@runBlocking RdTestGenerationResult(result.notEmptyCases, result.testSetsId)
    }

    fun render(
        testSetsId: Long,
        classUnderTest: ClassId,
        paramNames: MutableMap<ExecutableId, List<String>>,
        generateUtilClassFile: Boolean,
        testFramework: TestFramework,
        mockFramework: MockFramework,
        staticsMocking: StaticsMocking,
        forceStaticMocking: ForceStaticMocking,
        generateWarningsForStaticsMocking: Boolean,
        codegenLanguage: CodegenLanguage,
        parameterizedTestSource: ParametrizedTestSource,
        runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour,
        hangingTestSource: HangingTestsTimeout,
        enableTestsTimeout: Boolean,
        testClassPackageName: String
    ): Pair<String, UtilClassKind?> = runBlocking {
        val result = engineModel().render.startSuspending(
            ldef, RenderParams(
                    testSetsId,
                    kryoHelper.writeObject(classUnderTest),
                    kryoHelper.writeObject(paramNames),
                    generateUtilClassFile,
                    testFramework.id.lowercase(),
                    mockFramework.name,
                    codegenLanguage.name,
                    parameterizedTestSource.name,
                    staticsMocking.id,
                    kryoHelper.writeObject(forceStaticMocking),
                    generateWarningsForStaticsMocking,
                    runtimeExceptionTestsBehaviour.name,
                    hangingTestSource.timeoutMs,
                    enableTestsTimeout,
                    testClassPackageName
                )
            )
        result.generatedCode to kryoHelper.readObject(result.utilClassKind)
    }

    fun forceTermination() = runBlocking {
        configPath?.deleteIfExists()
        engineModel().stopProcess.start(Unit)
        current?.terminate()
    }

    fun writeSarif(reportFilePath: Path,
                   testSetsId: Long,
                   generatedTestsCode: String,
                   sourceFindingStrategy: SourceFindingStrategy
    ) = runBlocking {
        current!!.protocol.rdSourceFindingStrategy.let {
            it.getSourceFile.set { params ->
                DumbService.getInstance(project).runReadActionInSmartMode<String?> {
                    sourceFindingStrategy.getSourceFile(
                        params.classFqn,
                        params.extension
                    )?.canonicalPath
                }
            }
            it.getSourceRelativePath.set { params ->
                DumbService.getInstance(project).runReadActionInSmartMode<String> {
                    sourceFindingStrategy.getSourceRelativePath(
                        params.classFqn,
                        params.extension
                    )
                }
            }
            it.testsRelativePath.set { _ ->
                DumbService.getInstance(project).runReadActionInSmartMode<String> {
                    sourceFindingStrategy.testsRelativePath
                }
            }
        }
        engineModel().writeSarifReport.startSuspending(WriteSarifReportArguments(testSetsId, reportFilePath.pathString, generatedTestsCode))
    }

    fun generateTestsReport(model: GenerateTestsModel, eventLogMessage: String?): Triple<String, String?, Boolean> = runBlocking {
        val forceMockWarning = if (model.conflictTriggers[Conflict.ForceMockHappened] == true) {
            """
                    <b>Warning</b>: Some test cases were ignored, because no mocking framework is installed in the project.<br>
                    Better results could be achieved by <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.mockitoSuffix}">installing mocking framework</a>.
                """.trimIndent()
        } else null
        val forceStaticMockWarnings = if (model.conflictTriggers[Conflict.ForceStaticMockHappened] == true) {
            """
                    <b>Warning</b>: Some test cases were ignored, because mockito-inline is not installed in the project.<br>
                    Better results could be achieved by <a href="${TestReportUrlOpeningListener.prefix}${TestReportUrlOpeningListener.mockitoInlineSuffix}">configuring mockito-inline</a>.
                """.trimIndent()
        } else null
        val testFrameworkWarnings = if (model.conflictTriggers[Conflict.TestFrameworkConflict] == true) {
            """
                    <b>Warning</b>: There are several test frameworks in the project. 
                    To select run configuration, please refer to the documentation depending on the project build system:
                     <a href=" https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests">Gradle</a>, 
                     <a href=" https://maven.apache.org/surefire/maven-surefire-plugin/examples/providers.html">Maven</a> 
                     or <a href=" https://www.jetbrains.com/help/idea/run-debug-configuration.html#compound-configs">Idea</a>.
                """.trimIndent()
        } else null
        val result = engineModel().generateTestReport.startSuspending(
            GenerateTestReportArgs(
                eventLogMessage,
                model.testPackageName,
                model.isMultiPackage,
                forceMockWarning,
                forceStaticMockWarnings,
                testFrameworkWarnings,
                model.conflictTriggers.triggered
            )
        )

        return@runBlocking Triple(result.notifyMessage, result.statistics, result.hasWarnings)
    }

    init {
        ldef.onTermination {
            forceTermination()
        }
    }
}