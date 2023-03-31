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
import mu.KotlinLogging
import org.utbot.common.AbstractSettings
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.ApplicationContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.generated.*
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.intellij.plugin.UtbotBundle
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.TestReportUrlOpeningListener
import org.utbot.intellij.plugin.util.assertReadAccessNotAllowed
import org.utbot.intellij.plugin.util.methodDescription
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.generated.SettingForResult
import org.utbot.rd.generated.SettingsModel
import org.utbot.rd.generated.settingsModel
import org.utbot.rd.generated.synchronizationModel
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startBlocking
import org.utbot.sarif.SourceFindingStrategy
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

data class RdTestGenerationResult(val notEmptyCases: Int, val testSetsId: Long)

class EngineProcessInstantDeathException :
    InstantProcessDeathException(UtSettings.engineProcessDebugPort, UtSettings.runEngineProcessWithDebug)

class EngineProcess private constructor(val project: Project, private val classNameToPath: Map<String, String?>, rdProcess: ProcessWithRdServer) :
    AbstractProcess(rdProcess) {

    data class Params(
        val project: Project,
        val classNameToPath: Map<String, String?>
    )

    companion object : AbstractProcess.Companion<Params, EngineProcess>(
        displayName = "Engine",
        logConfigFileGetter = { UtSettings.engineProcessLogConfigFile },
        debugPortGetter = { UtSettings.engineProcessDebugPort },
        runWithDebugGetter = { UtSettings.runEngineProcessWithDebug },
        suspendExecutionInDebugModeGetter = { UtSettings.suspendEngineProcessExecutionInDebugMode },
        logConfigurationsDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogConfigurations"),
        logDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogs"),
        logConfigurationFileDeleteKey = "engine_process_appender_comment_key",
        logAppender = "EngineProcessAppender",
        currentLogFilename = "utbot-engine-current.log",
        logger = KotlinLogging.logger {}
    ) {
        private val pluginClasspath: String
            get() = (this.javaClass.classLoader as PluginClassLoader).classPath.baseUrls.joinToString(
                separator = File.pathSeparator,
                prefix = "\"",
                postfix = "\""
            )

        private const val startFileName = "org.utbot.framework.process.EngineProcessMainKt"

        override fun obtainProcessSpecificCommandLineArgs() = listOf(
            "-ea",
            "-cp",
            pluginClasspath,
            startFileName
        )

        override fun getWorkingDirectory() = WorkingDirService.provide().toFile()

        override fun createFromRDProcess(params: Params, rdProcess: ProcessWithRdServer) =
            EngineProcess(params.project, params.classNameToPath, rdProcess)

        override fun createInstantDeathException() = EngineProcessInstantDeathException()
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