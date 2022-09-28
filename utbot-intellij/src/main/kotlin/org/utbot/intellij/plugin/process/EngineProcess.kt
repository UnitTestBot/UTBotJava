package org.utbot.intellij.plugin.process

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.jetbrains.kotlin.scripting.resolve.classId
import org.utbot.common.AbstractSettings
import org.utbot.common.getPid
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.plugin.services.WorkingDirService
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
import java.util.*
import kotlin.io.path.pathString
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val logger = KotlinLogging.logger {}
private val engineProcessLogDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogs")

data class RdGTestenerationResult(val notEmptyCases: Int, val testSetsId: Long)

class EngineProcess(val lifetime: Lifetime) {
    private val id = Random.nextLong()
    private var count = 0

    companion object {
        private var configPath: Path? = null
        private fun getOrCreateLogConfig(): String {
            var realPath = configPath
            if (realPath == null) {
                synchronized(this) {
                    realPath = configPath
                    if (realPath == null) {
                        utBotTempDirectory.toFile().mkdirs()
                        configPath = utBotTempDirectory.toFile().resolve("EngineProcess_log4j2.xml").apply {
                            writeText(
                                """<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <ThresholdFilter level="ERROR"  onMatch="DENY"   onMismatch="DENY"/>
            <PatternLayout pattern="%d{HH:mm:ss.SSS} | %-5level | %c{1} | %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="error">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>"""
                            )
                        }.toPath()
                        realPath = configPath
                    }
                }
            }
            return realPath!!.pathString
        }
    }
    // because we cannot load idea bundled lifetime or it will break everything

    private fun debugArgument(): String {
        return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005".takeIf { Settings.runIdeaProcessWithDebug }
            ?: ""
    }

    private val kryoHelper = KryoHelper(lifetime)

    private suspend fun engineModel(): EngineProcessModel {
        lifetime.throwIfNotAlive()
        return lock.withLock {
            var proc = current

            if (proc == null) {
                proc = startUtProcessWithRdServer(lifetime) { port ->
                    val current = JdkInfoDefaultProvider().info
                    val required = JdkInfoService.jdkInfoProvider.info
                    val java =
                        JdkInfoService.jdkInfoProvider.info.path.resolve("bin${File.separatorChar}javaw").toString()
                    val cp = (this.javaClass.classLoader as PluginClassLoader).classPath.baseUrls.joinToString(
                        separator = ";",
                        prefix = "\"",
                        postfix = "\""
                    )
                    val classname = "org.utbot.framework.process.EngineMainKt"
                    val javaVersionSpecificArguments =
                        listOf("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", "--illegal-access=warn")
                            .takeIf { JdkInfoService.provide().version > 8 }
                            ?: emptyList()
                    val directory = WorkingDirService.provide().toFile()
                    val log4j2ConfigFile = "\"-Dlog4j2.configurationFile=${getOrCreateLogConfig()}\""
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
        engineModel().setupUtContext.startSuspending(lifetime, SetupContextParams(classpathForUrlsClassloader))
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
            lifetime,
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
    ): RdGTestenerationResult = runBlocking {
        val result = engineModel().generate.startSuspending(
            lifetime,
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

        return@runBlocking RdGTestenerationResult(result.notEmptyCases, result.testSetsId)
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
                lifetime, RenderParams(
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
        engineModel().stopProcess.start(Unit)
        current?.terminate()
        engineModel().writeSarifReport
    }

    fun writeSarif(reportFilePath: Path,
                   testSetsId: Long,
                   generatedTestsCode: String,
                   sourceFindingStrategy: SourceFindingStrategy
    ) = runBlocking {
        current!!.protocol.rdSourceFindingStrategy.let {
            it.getSourceFile.set { params ->
                ApplicationManager.getApplication().runReadAction<String?> {
                    sourceFindingStrategy.getSourceFile(
                        params.classFqn,
                        params.extension
                    )?.canonicalPath
                }
            }
            it.getSourceRelativePath.set { params ->
                ApplicationManager.getApplication().runReadAction<String> {
                    sourceFindingStrategy.getSourceRelativePath(
                        params.classFqn,
                        params.extension
                    )
                }
            }
            it.testsRelativePath.set { _ ->
                ApplicationManager.getApplication().runReadAction<String> {
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
        lifetime.onTermination {
            current?.terminate()
        }
    }
}