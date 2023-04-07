package org.utbot.intellij.plugin.process

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.jetbrains.rd.util.ConcurrentHashMap
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.common.*
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.*
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.AbstractRDProcessCompanion
import org.utbot.framework.process.generated.*
import org.utbot.framework.process.generated.MethodDescription
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.intellij.plugin.UtbotBundle
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.util.assertReadAccessNotAllowed
import org.utbot.intellij.plugin.util.methodDescription
import org.utbot.rd.*
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.SettingForResult
import org.utbot.rd.generated.SettingsModel
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.loggers.overrideDefaultRdLoggerFactoryWithKLogger
import org.utbot.sarif.SourceFindingStrategy
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.pathString
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val engineProcessLogConfigurationsDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogConfigurations").also { it.mkdirs() }
private val logger = KotlinLogging.logger {}.also { overrideDefaultRdLoggerFactoryWithKLogger(it) }
private val engineProcessLogDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogs").also { it.mkdirs() }

private const val configurationFileDeleteKey = "delete_this_comment_key"
private const val deleteOpenComment = "<!--$configurationFileDeleteKey"
private const val deleteCloseComment = "$configurationFileDeleteKey-->"

private fun createEngineProcessLog4j2Config(): File {
    val customFile = File(UtSettings.engineProcessLogConfigFile)

    val log4j2ConfigFile =
        if (customFile.exists()) customFile
        else Files.createTempFile(engineProcessLogConfigurationsDirectory.toPath(), null, ".xml").toFile()

    EngineProcess::class.java.classLoader.getResourceAsStream("log4j2.xml")?.use { logConfig ->
        val resultConfig = logConfig.readBytes().toString(Charset.defaultCharset())
            .replace(Regex("$deleteOpenComment|$deleteCloseComment"), "")
            .replace("ref=\"IdeaAppender\"", "ref=\"EngineProcessAppender\"")
            .replace("\${env:UTBOT_LOG_DIR}", engineProcessLogDirectory.canonicalPath.trimEnd(File.separatorChar) + File.separatorChar)
        Files.copy(
            resultConfig.byteInputStream(),
            log4j2ConfigFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
    return log4j2ConfigFile
}

private val log4j2ConfigFile: File = createEngineProcessLog4j2Config()

private val log4j2ConfigSwitch = "-Dlog4j2.configurationFile=${log4j2ConfigFile.canonicalPath}"

private val pluginClasspath: String
    get() = (EngineProcess::class.java.classLoader as PluginClassLoader).classPath.baseUrls.joinToString(
        separator = File.pathSeparator,
        prefix = "\"",
        postfix = "\""
    )

private const val startFileName = "org.utbot.framework.process.EngineProcessMainKt"

data class RdTestGenerationResult(val notEmptyCases: Int, val testSetsId: Long)

class EngineProcessInstantDeathException :
    InstantProcessDeathException(UtSettings.engineProcessDebugPort, UtSettings.runEngineProcessWithDebug)

class EngineProcess private constructor(val project: Project, private val classNameToPath: Map<String, String?>, rdProcess: ProcessWithRdServer) :
    ProcessWithRdServer by rdProcess {
    companion object : AbstractRDProcessCompanion(
        debugPort = UtSettings.engineProcessDebugPort,
        runWithDebug = UtSettings.runEngineProcessWithDebug,
        suspendExecutionInDebugMode = UtSettings.suspendEngineProcessExecutionInDebugMode,
        processSpecificCommandLineArgs = listOf("-ea", log4j2ConfigSwitch, "-cp", pluginClasspath, startFileName)
    ) {
        fun createBlocking(project: Project, classNameToPath: Map<String, String?>): EngineProcess = runBlocking { EngineProcess(project, classNameToPath) }

        suspend operator fun invoke(project: Project, classNameToPath: Map<String, String?>): EngineProcess =
            LifetimeDefinition().terminateOnException { lifetime ->
                val rdProcess = startUtProcessWithRdServer(lifetime) { port ->
                    val cmd = obtainProcessCommandLine(port)
                    val directory = WorkingDirService.provide().toFile()
                    val builder = ProcessBuilder(cmd).directory(directory)
                    val process = builder.start()

                    logger.info { "Engine process started with PID = ${process.getPid}" }
                    logger.info { "Engine process log directory - ${engineProcessLogDirectory.canonicalPath}" }
                    logger.info { "Engine process log file - ${engineProcessLogDirectory.resolve("utbot-engine-current.log")}" }
                    logger.info { "Log4j2 configuration file path - ${log4j2ConfigFile.canonicalPath}" }

                    if (!process.isAlive) {
                        throw EngineProcessInstantDeathException()
                    }

                    process
                }
                rdProcess.awaitProcessReady()

                return EngineProcess(project, classNameToPath, rdProcess)
            }
    }

    private val engineModel: EngineProcessModel = onSchedulerBlocking { protocol.engineProcessModel }
    private val instrumenterAdapterModel: RdInstrumenterAdapter = onSchedulerBlocking { protocol.rdInstrumenterAdapter }
    private val sourceFindingModel: RdSourceFindingStrategy = onSchedulerBlocking { protocol.rdSourceFindingStrategy }
    private val settingsModel: SettingsModel = onSchedulerBlocking { protocol.settingsModel }

    private val kryoHelper = KryoHelper(lifetime)
    private val sourceFindingStrategies = ConcurrentHashMap<Long, SourceFindingStrategy>()

    fun setupUtContext(classpathForUrlsClassloader: List<String>) {
        assertReadAccessNotAllowed()
        engineModel.setupUtContext.startBlocking(SetupContextParams(classpathForUrlsClassloader))
    }

    fun getSpringBeanQualifiedNames(classpathList: List<String>, config: String, useSpringAnalyzer: Boolean): List<String> {
        assertReadAccessNotAllowed()
        return engineModel.getSpringBeanQualifiedNames.startBlocking(
            GetSpringBeanQualifiedNamesParams(classpathList.toTypedArray(), config, useSpringAnalyzer)
        ).toList()
    }

    private fun computeSourceFileByClass(params: ComputeSourceFileByClassArguments): String =
        DumbService.getInstance(project).runReadActionInSmartMode<String?> {
            val scope = GlobalSearchScope.allScope(project)
            // JavaFileManager requires canonical name as it is said in import
            val psiClass = JavaFileManager.getInstance(project).findClass(params.canonicalClassName, scope)
            val sourceFile = psiClass?.navigationElement?.containingFile?.virtualFile?.canonicalPath

            logger.debug { "computeSourceFileByClass result: $sourceFile" }
            sourceFile ?: classNameToPath[params.canonicalClassName]
        }

    fun createTestGenerator(
        buildDir: List<String>,
        classPath: String?,
        dependencyPaths: String,
        jdkInfo: JdkInfo,
        applicationContext: ApplicationContext,
        isCancelled: (Unit) -> Boolean
    ) {
        assertReadAccessNotAllowed()

        engineModel.isCancelled.set(handler = isCancelled)
        instrumenterAdapterModel.computeSourceFileByClass.set(handler = this::computeSourceFileByClass)

        val params = TestGeneratorParams(
            buildDir.toTypedArray(),
            classPath,
            dependencyPaths,
            JdkInfo(jdkInfo.path.pathString, jdkInfo.version),
            kryoHelper.writeObject(applicationContext)
        )
        engineModel.createTestGenerator.startBlocking(params)
    }

    fun obtainClassId(canonicalName: String): ClassId {
        assertReadAccessNotAllowed()
        return kryoHelper.readObject(engineModel.obtainClassId.startBlocking(canonicalName))
    }

    fun findMethodsInClassMatchingSelected(clazzId: ClassId, srcMethods: List<MemberInfo>): List<ExecutableId> {
        assertReadAccessNotAllowed()

        val srcDescriptions = runReadAction { srcMethods.map { it.methodDescription() } }
        val rdDescriptions = srcDescriptions.map { MethodDescription(it.name, it.containingClass, it.parameterTypes) }
        val binaryClassId = kryoHelper.writeObject(clazzId)
        val arguments = FindMethodsInClassMatchingSelectedArguments(binaryClassId, rdDescriptions)
        val result = engineModel.findMethodsInClassMatchingSelected.startBlocking(arguments)

        return kryoHelper.readObject(result.executableIds)
    }

    fun findMethodParamNames(classId: ClassId, methods: List<MemberInfo>): Map<ExecutableId, List<String>> {
        assertReadAccessNotAllowed()

        val bySignature = executeWithTimeoutSuspended {
            DumbService.getInstance(project).runReadActionInSmartMode(Computable {
                methods.map { it.methodDescription() to it.paramNames() }
            })
        }
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
        assertReadAccessNotAllowed()
        val params = GenerateParams(
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
        projectType: ProjectType,
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
        assertReadAccessNotAllowed()
        val params = RenderParams(
            testSetsId,
            kryoHelper.writeObject(classUnderTest),
            projectType.toString(),
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
        assertReadAccessNotAllowed()

        val params = WriteSarifReportArguments(testSetsId, reportFilePath.pathString, generatedTestsCode)

        sourceFindingStrategies[testSetsId] = sourceFindingStrategy
        return engineModel.writeSarifReport.startBlocking(params)
    }

    fun generateTestsReport(model: GenerateTestsModel, eventLogMessage: String?): Triple<String, String?, Boolean> {
        assertReadAccessNotAllowed()

        val forceMockWarning = UtbotBundle.takeIf(
            "test.report.force.mock.warning",
            TestReportUrlOpeningListener.prefix,
            TestReportUrlOpeningListener.mockitoSuffix
        ) { model.conflictTriggers[Conflict.ForceMockHappened] == true }
        val forceStaticMockWarnings = UtbotBundle.takeIf(
            "test.report.force.static.mock.warning",
            TestReportUrlOpeningListener.prefix,
            TestReportUrlOpeningListener.mockitoInlineSuffix
        ) { model.conflictTriggers[Conflict.ForceStaticMockHappened] == true }
        val testFrameworkWarnings =
            UtbotBundle.takeIf("test.report.test.framework.warning") { model.conflictTriggers[Conflict.TestFrameworkConflict] == true }
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
            protocol.synchronizationModel.stopProcess.fire(Unit)
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

    fun <T> executeWithTimeoutSuspended(block: () -> T): T {
        try {
            assertReadAccessNotAllowed()
            protocol.synchronizationModel.suspendTimeoutTimer.startBlocking(true)
            return block()
        }
        finally {
            assertReadAccessNotAllowed()
            protocol.synchronizationModel.suspendTimeoutTimer.startBlocking(false)
        }
    }
}