package org.utbot.intellij.plugin.process

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.common.*
import org.utbot.common.PathUtil.toPathOrNull
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.runIdeaProcessWithDebug
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.OpenModulesContainer
import org.utbot.framework.process.generated.*
import org.utbot.framework.process.generated.Signature
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.intellij.plugin.UtbotBundle
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.util.assertIsNonDispatchThread
import org.utbot.intellij.plugin.util.assertIsReadAccessAllowed
import org.utbot.intellij.plugin.util.signature
import org.utbot.rd.*
import org.utbot.rd.loggers.UtRdKLoggerFactory
import org.utbot.sarif.SourceFindingStrategy
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import kotlin.io.path.pathString
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val engineProcessLogConfigurations = utBotTempDirectory.toFile().resolve("rdEngineProcessLogConfigurations")
private val logger = KotlinLogging.logger {}
private val engineProcessLogDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogs")

data class RdTestGenerationResult(val notEmptyCases: Int, val testSetsId: Long)

class EngineProcess private constructor(val project: Project, rdProcess: ProcessWithRdServer) :
    ProcessWithRdServer by rdProcess {
    companion object {
        private val log4j2ConfigFile: File

        init {
            engineProcessLogDirectory.mkdirs()
            engineProcessLogConfigurations.mkdirs()
            Logger.set(Lifetime.Eternal, UtRdKLoggerFactory(logger))

            val customFile = File(UtSettings.ideaProcessLogConfigFile)

            if (customFile.exists()) {
                log4j2ConfigFile = customFile
            } else {
                log4j2ConfigFile = Files.createTempFile(engineProcessLogConfigurations.toPath(), null, ".xml").toFile()
                log4j2ConfigFile.deleteOnExit()
                this.javaClass.classLoader.getResourceAsStream("log4j2.xml")?.use { logConfig ->
                    val resultConfig = logConfig.readBytes().toString(Charset.defaultCharset())
                        .replace("ref=\"IdeaAppender\"", "ref=\"EngineProcessAppender\"")
                    Files.copy(
                        resultConfig.byteInputStream(),
                        log4j2ConfigFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }

        private val log4j2ConfigSwitch = "-Dlog4j2.configurationFile=${log4j2ConfigFile.canonicalPath}"

        private val debugArgument: String?
            get() = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=$engineProcessDebugPort"
                .takeIf { runIdeaProcessWithDebug }

        private val javaExecutablePathString: Path
            get() = JdkInfoService.jdkInfoProvider.info.path.resolve("bin${File.separatorChar}${osSpecificJavaExecutable()}")

        private val pluginClasspath: String
            get() = (this.javaClass.classLoader as PluginClassLoader).classPath.baseUrls.joinToString(
                separator = File.pathSeparator,
                prefix = "\"",
                postfix = "\""
            )

        private const val startFileName = "org.utbot.framework.process.EngineMainKt"

        private fun obtainEngineProcessCommandLine(port: Int) = buildList<String> {
            add(javaExecutablePathString.pathString)
            add("-ea")
            OpenModulesContainer.javaVersionSpecificArguments?.let { addAll(it) }
            debugArgument?.let { add(it) }
            add(log4j2ConfigSwitch)
            add("-cp")
            add(pluginClasspath)
            add(startFileName)
            add(rdPortArgument(port))
        }

        fun createBlocking(project: Project): EngineProcess = runBlocking { EngineProcess(project) }

        suspend operator fun invoke(project: Project): EngineProcess =
            LifetimeDefinition().terminateOnException { lifetime ->
                val rdProcess = startUtProcessWithRdServer(lifetime) { port ->
                    val cmd = obtainEngineProcessCommandLine(port)
                    val directory = WorkingDirService.provide().toFile()
                    val builder = ProcessBuilder(cmd).directory(directory)
                    val formatted = dateTimeFormatter.format(LocalDateTime.now())
                    val logFile = File(engineProcessLogDirectory, "$formatted.log")

                    builder.redirectOutput(logFile)
                    builder.redirectError(logFile)

                    val process = builder.start()

                    logger.info { "Engine process started with PID = ${process.getPid}" }
                    logger.info { "Engine process log file - ${logFile.canonicalPath}" }
                    process
                }
                rdProcess.awaitSignal()

                return EngineProcess(project, rdProcess)
            }
    }

    private val engineModel: EngineProcessModel = onSchedulerBlocking { protocol.engineProcessModel }
    private val instrumenterAdapterModel: RdInstrumenterAdapter = onSchedulerBlocking { protocol.rdInstrumenterAdapter }
    private val sourceFindingModel: RdSourceFindingStrategy = onSchedulerBlocking { protocol.rdSourceFindingStrategy }
    private val settingsModel: SettingsModel = onSchedulerBlocking { protocol.settingsModel }

    private val kryoHelper = KryoHelper(lifetime)
    private val sourceFindingStrategies = ConcurrentHashMap<Long, SourceFindingStrategy>()

    fun setupUtContext(classpathForUrlsClassloader: List<String>) {
        engineModel.setupUtContext.start(lifetime, SetupContextParams(classpathForUrlsClassloader))
    }

    private fun computeSourceFileByClass(params: ComputeSourceFileByClassArguments): String =
        DumbService.getInstance(project).runReadActionInSmartMode<String?> {
            val scope = GlobalSearchScope.allScope(project)
            val psiClass = JavaFileManager.getInstance(project).findClass(params.className, scope)
            val sourceFile = psiClass?.navigationElement?.containingFile?.virtualFile?.canonicalPath

            logger.debug { "computeSourceFileByClass result: $sourceFile" }
            sourceFile
        }

    fun createTestGenerator(
        buildDir: List<String>,
        classPath: String?,
        dependencyPaths: String,
        jdkInfo: JdkInfo,
        isCancelled: (Unit) -> Boolean
    ) {
        engineModel.isCancelled.set(handler = isCancelled)
        instrumenterAdapterModel.computeSourceFileByClass.set(handler = this::computeSourceFileByClass)

        val params = TestGeneratorParams(
            buildDir.toTypedArray(),
            classPath,
            dependencyPaths,
            JdkInfo(jdkInfo.path.pathString, jdkInfo.version)
        )
        engineModel.createTestGenerator.start(lifetime, params)
    }

    fun obtainClassId(canonicalName: String): ClassId {
        assertIsNonDispatchThread()
        return kryoHelper.readObject(engineModel.obtainClassId.startBlocking(canonicalName))
    }

    fun findMethodsInClassMatchingSelected(clazzId: ClassId, srcMethods: List<MemberInfo>): List<ExecutableId> {
        assertIsNonDispatchThread()
        assertIsReadAccessAllowed()

        val srcSignatures = srcMethods.map { it.signature() }
        val rdSignatures = srcSignatures.map { Signature(it.name, it.parameterTypes) }
        val binaryClassId = kryoHelper.writeObject(clazzId)
        val arguments = FindMethodsInClassMatchingSelectedArguments(binaryClassId, rdSignatures)
        val result = engineModel.findMethodsInClassMatchingSelected.startBlocking(arguments)

        return kryoHelper.readObject(result.executableIds)
    }

    fun findMethodParamNames(classId: ClassId, methods: List<MemberInfo>): Map<ExecutableId, List<String>> {
        assertIsNonDispatchThread()
        assertIsReadAccessAllowed()

        val bySignature = methods.associate { it.signature() to it.paramNames() }
        val arguments = FindMethodParamNamesArguments(
            kryoHelper.writeObject(classId),
            kryoHelper.writeObject(bySignature)
        )
        val result = engineModel.findMethodParamNames.startBlocking(arguments).paramNames

        return kryoHelper.readObject(result)
    }

    private fun MemberInfo.paramNames(): List<String> = (this.member as PsiMethod).parameterList.parameters.map {
        if (it.name.startsWith("\$this"))
        // If member is Kotlin extension function, name of first argument isn't good for further usage,
        // so we better choose name based on type of receiver.
        //
        // There seems no API to check whether parameter is an extension receiver by PSI
            it.type.presentableText
        else
            it.name
    }

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
    ): RdTestGenerationResult {
        assertIsNonDispatchThread()
        val params = GenerateParams(
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
        val result = engineModel.generate.startBlocking(params)

        return RdTestGenerationResult(result.notEmptyCases, result.testSetsId)
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
    ): Pair<String, UtilClassKind?> {
        assertIsNonDispatchThread()
        val params = RenderParams(
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
        val result = engineModel.render.startBlocking(params)
        val realUtilClassKind = result.utilClassKind?.let {
            if (UtilClassKind.RegularUtUtils(codegenLanguage).javaClass.simpleName == it)
                UtilClassKind.RegularUtUtils(codegenLanguage)
            else
                UtilClassKind.UtUtilsWithMockito(codegenLanguage)
        }

        return result.generatedCode to realUtilClassKind
    }

    private fun getSourceFile(params: SourceStrategyMethodArgs): String? =
        DumbService.getInstance(project).runReadActionInSmartMode<String?> {
            sourceFindingStrategies[params.testSetId]!!.getSourceFile(
                params.classFqn,
                params.extension
            )?.canonicalPath
        }

    private fun getSourceRelativePath(params: SourceStrategyMethodArgs): String =
        DumbService.getInstance(project).runReadActionInSmartMode<String> {
            sourceFindingStrategies[params.testSetId]!!.getSourceRelativePath(
                params.classFqn,
                params.extension
            )
        }

    private fun testsRelativePath(testSetId: Long): String =
        DumbService.getInstance(project).runReadActionInSmartMode<String> {
            sourceFindingStrategies[testSetId]!!.testsRelativePath
        }

    private fun initSourceFindingStrategies() {
        sourceFindingModel.getSourceFile.set(handler = this::getSourceFile)
        sourceFindingModel.getSourceRelativePath.set(handler = this::getSourceRelativePath)
        sourceFindingModel.testsRelativePath.set(handler = this::testsRelativePath)
    }

    fun writeSarif(
        reportFilePath: Path,
        testSetsId: Long,
        generatedTestsCode: String,
        sourceFindingStrategy: SourceFindingStrategy
    ): String {
        assertIsNonDispatchThread()

        val params = WriteSarifReportArguments(testSetsId, reportFilePath.pathString, generatedTestsCode)

        sourceFindingStrategies[testSetsId] = sourceFindingStrategy
        return engineModel.writeSarifReport.startBlocking(params)
    }

    fun generateTestsReport(model: GenerateTestsModel, eventLogMessage: String?): Triple<String, String?, Boolean> {
        assertIsNonDispatchThread()

        // todo unforce loading

        val forceMockWarning = UtbotBundle.message(
            "test.report.force.mock.warning",
            TestReportUrlOpeningListener.prefix,
            TestReportUrlOpeningListener.mockitoSuffix
        ).takeIf { model.conflictTriggers[Conflict.ForceMockHappened] == true }
        val forceStaticMockWarnings = UtbotBundle.message(
            "test.report.force.static.mock.warning",
            TestReportUrlOpeningListener.prefix,
            TestReportUrlOpeningListener.mockitoInlineSuffix
        ).takeIf { model.conflictTriggers[Conflict.ForceStaticMockHappened] == true }
        val testFrameworkWarnings = UtbotBundle.message("test.report.test.framework.warning")
            .takeIf { model.conflictTriggers[Conflict.TestFrameworkConflict] == true }
        val params = GenerateTestReportArgs(
            eventLogMessage,
            model.testPackageName,
            model.isMultiPackage,
            forceMockWarning,
            forceStaticMockWarnings,
            testFrameworkWarnings,
            model.conflictTriggers.anyTriggered
        )
        val result = engineModel.generateTestReport.startBlocking(params)

        return Triple(result.notifyMessage, result.statistics, result.hasWarnings)
    }

    init {
        lifetime.onTermination {
            engineModel.stopProcess.start(Unit)
        }
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
        initSourceFindingStrategies()
    }
}