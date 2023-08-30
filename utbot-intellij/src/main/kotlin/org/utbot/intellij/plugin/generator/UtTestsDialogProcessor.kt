package org.utbot.intellij.plugin.generator

import com.intellij.openapi.application.*
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskManager
import com.intellij.task.impl.ModuleBuildTaskImpl
import com.intellij.task.impl.ModuleFilesBuildTaskImpl
import com.intellij.task.impl.ProjectTaskList
import com.intellij.util.PathsList
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.nullize
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.pathString
import mu.KotlinLogging
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.all
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.base.util.module
import org.utbot.framework.CancellationStrategyType.CANCEL_EVERYTHING
import org.utbot.framework.CancellationStrategyType.NONE
import org.utbot.framework.CancellationStrategyType.SAVE_PROCESSED_RESULTS
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ProjectType.*
import org.utbot.framework.context.simple.SimpleApplicationContext
import org.utbot.framework.context.simple.SimpleMockerContext
import org.utbot.framework.context.spring.SpringApplicationContextImpl
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.SpringSettings.*
import org.utbot.framework.plugin.api.SpringConfiguration.*
import org.utbot.framework.plugin.api.SpringTestType.*
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.framework.plugin.api.util.withStaticsSubstitutionRequired
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.SpringAnalyzerTask
import org.utbot.instrumentation.instrumentation.spring.SpringUtExecutionInstrumentation
import org.utbot.intellij.plugin.generator.CodeGenerationController.generateTests
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.intellij.plugin.process.RdTestGenerationResult
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.intellij.plugin.util.IntelliJApiHelper
import org.utbot.intellij.plugin.util.PsiClassHelper
import org.utbot.intellij.plugin.util.isAbstract
import org.utbot.intellij.plugin.util.binaryName
import org.utbot.intellij.plugin.util.PluginJdkInfoProvider
import org.utbot.intellij.plugin.util.PluginWorkingDirProvider
import org.utbot.intellij.plugin.util.assertIsNonDispatchThread
import org.utbot.intellij.plugin.util.extractClassMethodsIncludingNested
import org.utbot.rd.terminateOnException

object UtTestsDialogProcessor {
    private val logger = KotlinLogging.logger {}

    enum class ProgressRange(val from : Double, val to: Double) {
        INITIALIZATION(from = 0.01, to = 0.1),
        SOLVING(from = 0.1, to = 0.9),
        CODEGEN(from = 0.9, to = 0.95),
        SARIF(from = 0.95, to = 1.0)
    }

    fun updateIndicator(indicator: ProgressIndicator, range : ProgressRange, text: String? = null, fraction: Double? = null) {
        invokeLater {
            if (indicator.isCanceled) return@invokeLater
            text?.let { indicator.text = it }
            fraction?.let {
                indicator.fraction =
                    indicator.fraction.coerceAtLeast(range.from + (range.to - range.from) * fraction.coerceIn(0.0, 1.0))
            }
            logger.debug("Phase ${indicator.text} with progress ${String.format("%.2f",indicator.fraction)}")
        }
    }


    fun createDialogAndGenerateTests(
        project: Project,
        srcClasses: Set<PsiClass>,
        extractMembersFromSrcClasses: Boolean,
        focusedMethods: Set<MemberInfo>,
    ) {
        createDialog(project, srcClasses, extractMembersFromSrcClasses, focusedMethods)?.let {
            if (it.showAndGet()) createTests(project, it.model)
        }
    }

    private fun createDialog(
        project: Project,
        srcClasses: Set<PsiClass>,
        extractMembersFromSrcClasses: Boolean,
        focusedMethods: Set<MemberInfo>,
    ): GenerateTestsDialogWindow? {
        val srcModule = findSrcModule(srcClasses)
        val testModules = srcModule.testModules(project)

        JdkInfoService.jdkInfoProvider = PluginJdkInfoProvider(project)
        // we want to start the instrumented process in the same directory as the test runner
        WorkingDirService.workingDirProvider = PluginWorkingDirProvider(project)

        val model = GenerateTestsModel(
            project,
            srcModule,
            testModules,
            srcClasses,
            extractMembersFromSrcClasses,
            focusedMethods,
            project.service<Settings>().generationTimeoutInMillis
        )
        if (model.getAllTestSourceRoots().isEmpty() && project.isBuildWithGradle) {
            val errorMessage = """
                <html>No test source roots found in the project.<br>
                Please, <a href="https://www.jetbrains.com/help/idea/testing.html#add-test-root">create or configure</a> at least one test source root.
            """.trimIndent()
            showErrorDialogLater(project, errorMessage, "Test source roots not found")
            return null
        }

        return GenerateTestsDialogWindow(model)
    }

    private fun compile(
        project: Project,
        files: Array<VirtualFile>,
        springConfigClass: PsiClass?,
    ): Promise<ProjectTaskManager.Result> {
        val buildTasks = runReadAction {
            // For Maven project narrow compile scope may not work, see https://github.com/UnitTestBot/UTBotJava/issues/2021.
            // For Spring project classes may contain `@ComponentScan` annotations, so we need to compile the whole module.
            val isMavenProject = MavenProjectsManager.getInstance(project)?.hasProjects() ?: false
            val isSpringProject = springConfigClass != null
            val wholeModules = isMavenProject || isSpringProject

            ContainerUtil.map<Map.Entry<Module?, List<VirtualFile>>, ProjectTask>(
                Arrays.stream(files).collect(Collectors.groupingBy { file: VirtualFile ->
                    ProjectFileIndex.getInstance(project).getModuleForFile(file, false)
                }).entries
            ) { (key, value): Map.Entry<Module?, List<VirtualFile>?> ->
                if (wholeModules) {
                    // This is a specific case, we have to compile the whole module
                    ModuleBuildTaskImpl(key!!, false)
                } else {
                    // Compile only chosen classes and their dependencies before generation.
                    ModuleFilesBuildTaskImpl(key, false, value)
                }
            }
        }
        return ProjectTaskManager.getInstance(project).run(ProjectTaskList(buildTasks))
    }

    private fun createTests(project: Project, model: GenerateTestsModel) {
        val springConfigClass =
            when (val settings = model.springSettings) {
                is AbsentSpringSettings -> null
                is PresentSpringSettings ->
                    when (val config = settings.configuration) {
                        is JavaBasedConfiguration -> {
                            PsiClassHelper
                                .findClass(config.configBinaryName, project)
                                ?: error("Cannot find configuration class ${config.configBinaryName}.")
                        }
                        // TODO: for XML config we also need to compile module containing,
                        //  since it may reference classes from that module
                        is XMLConfiguration -> null
                    }
            }

        val filesToCompile = (model.srcClasses + listOfNotNull(springConfigClass))
            .map { it.containingFile.virtualFile }
            .toTypedArray()

        compile(project, filesToCompile, springConfigClass).onSuccess { task ->
            if (task.hasErrors() || task.isAborted)
                return@onSuccess

            (object : Task.Backgroundable(project, "Generate tests") {

                override fun run(indicator: ProgressIndicator) {
                    assertIsNonDispatchThread()
                    if (!LockFile.lock()) {
                        return
                    }

                    UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis = model.hangingTestsTimeout.timeoutMs
                    UtSettings.useCustomJavaDocTags = model.commentStyle == JavaDocCommentStyle.CUSTOM_JAVADOC_TAGS
                    UtSettings.summaryGenerationType = model.summariesGenerationType

                    fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))

                    try {
                        logger.info { "Collecting information phase started at ${now()}" }
                        val secondsTimeout = TimeUnit.MILLISECONDS.toSeconds(model.timeout)

                        indicator.isIndeterminate = false
                        updateIndicator(indicator, ProgressRange.INITIALIZATION, "Generate tests: starting engine", 0.0)

                        // TODO sometimes preClasspathCollectionPromises get stuck, even though all
                        //  needed dependencies get installed, we need to figure out why that happens
                        try {
                            model.preClasspathCollectionPromises
                                .all()
                                .blockingGet(10, TimeUnit.SECONDS)
                        } catch (e: java.util.concurrent.TimeoutException) {
                            logger.warn { "preClasspathCollectionPromises are stuck over 10 seconds, ignoring them" }
                        }

                        val buildPaths = ReadAction
                            .nonBlocking<BuildPaths?> {
                                findPaths(listOf(findSrcModule(model.srcClasses)) + when (model.projectType) {
                                    Spring -> listOfNotNull(
                                        model.testModule, // needed so we can use `TestContextManager` from `spring-test`
                                        springConfigClass?.let { it.module ?: error("Module for Spring configuration class not found") }
                                    )
                                    else -> emptyList()
                                })
                            }
                            .executeSynchronously()
                            ?: return

                        val (buildDirs, classpath, classpathList, pluginJarsPath) = buildPaths

                        val testSetsByClass = mutableMapOf<PsiClass, RdTestGenerationResult>()
                        val psi2KClass = mutableMapOf<PsiClass, ClassId>()
                        var processedClasses = 0
                        val totalClasses = model.srcClasses.size
                        val classNameToPath = runReadAction {
                            model.srcClasses.associate { psiClass ->
                                psiClass.binaryName to psiClass.containingFile.virtualFile.canonicalPath
                            }
                        }

                        val mockFrameworkInstalled = model.mockFramework.isInstalled
                        val staticMockingConfigured = model.staticsMocking.isConfigured

                        val process = EngineProcess.createBlocking(project, classNameToPath)
                        updateIndicator(indicator, ProgressRange.INITIALIZATION, fraction = 0.2)

                        process.terminateOnException { _ ->
                            val classpathForClassLoader = buildDirs + classpathList + when (model.projectType) {
                                Spring -> listOf(SpringUtExecutionInstrumentation.springCommonsJar.path)
                                else -> emptyList<String>()
                            }

                            process.setupUtContext(classpathForClassLoader)
                            val simpleApplicationContext = SimpleApplicationContext(
                                SimpleMockerContext(mockFrameworkInstalled, staticMockingConfigured)
                            )
                            val applicationContext = when (model.projectType) {
                                Spring -> {
                                    val beanDefinitions =
                                        when (val settings = model.springSettings) {
                                            is AbsentSpringSettings -> emptyList()
                                            is PresentSpringSettings -> {
                                                process.perform(
                                                    SpringAnalyzerTask(
                                                        classpath = classpathForClassLoader,
                                                        settings = settings
                                                    )
                                                )
                                            }
                                        }

                                    val clarifiedBeanDefinitions =
                                        clarifyBeanDefinitionReturnTypes(beanDefinitions, project)

                                    SpringApplicationContextImpl(
                                        simpleApplicationContext,
                                        clarifiedBeanDefinitions,
                                        model.springTestType,
                                        model.springSettings,
                                    )
                                }
                                else -> simpleApplicationContext
                            }
                            updateIndicator(indicator, ProgressRange.INITIALIZATION, fraction = 0.25)
                            process.createTestGenerator(
                                buildDirs,
                                classpath,
                                pluginJarsPath.joinToString(separator = File.pathSeparator),
                                JdkInfoService.provide(),
                                applicationContext,
                            ) {
                                ApplicationManager.getApplication().runReadAction(Computable {
                                    indicator.isCanceled
                                })
                            }
                            updateIndicator(indicator, ProgressRange.INITIALIZATION, fraction = 1.0)

                            for (srcClass in model.srcClasses) {
                                if (indicator.isCanceled) {
                                    when (UtSettings.cancellationStrategyType) {
                                        NONE -> {}
                                        SAVE_PROCESSED_RESULTS,
                                        CANCEL_EVERYTHING -> break
                                    }
                                }

                                val (methods, classNameForLog) = process.executeWithTimeoutSuspended {
                                    var binaryName = ""
                                    var srcMethods: List<MemberInfo> = emptyList()
                                    var srcNameForLog: String? = null
                                    DumbService.getInstance(project)
                                        .runReadActionInSmartMode(Computable {
                                            binaryName = srcClass.binaryName
                                            srcNameForLog = srcClass.name
                                            srcMethods = if (model.extractMembersFromSrcClasses) {
                                                val chosenMethods =
                                                    model.selectedMembers.filter { it.member is PsiMethod }
                                                val chosenNestedClasses =
                                                    model.selectedMembers.mapNotNull { it.member as? PsiClass }
                                                chosenMethods + chosenNestedClasses.flatMap {
                                                    it.extractClassMethodsIncludingNested(false)
                                                }
                                            } else {
                                                srcClass.extractClassMethodsIncludingNested(false)
                                            }
                                        })
                                    val classId = process.obtainClassId(binaryName)
                                    psi2KClass[srcClass] = classId
                                    process.findMethodsInClassMatchingSelected(
                                        classId,
                                        srcMethods
                                    ) to srcNameForLog
                                }

                                if (methods.isEmpty()) {
                                    logger.error { "No methods matching selected found in class $classNameForLog." }
                                    continue
                                }

                                logger.info { "Collecting information phase finished at ${now()}" }

                                updateIndicator(
                                    indicator,
                                    ProgressRange.SOLVING,
                                    "Generate test cases for class $classNameForLog",
                                    processedClasses.toDouble() / totalClasses
                                )

                                val searchDirectory = ReadAction
                                    .nonBlocking<Path> {
                                        project.basePath?.let { Paths.get(it) }
                                            ?: Paths.get(srcClass.containingFile.virtualFile.parent.path)
                                    }
                                    .executeSynchronously()

                                val taintConfigPath = getTaintConfigPath(project)

                                withStaticsSubstitutionRequired(true) {
                                    val startTime = System.currentTimeMillis()
                                    val timerHandler =
                                        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
                                            val innerTimeoutRatio =
                                                ((System.currentTimeMillis() - startTime).toDouble() / model.timeout)
                                                    .coerceIn(0.0, 1.0)
                                            updateIndicator(
                                                indicator,
                                                ProgressRange.SOLVING,
                                                "Generate test cases for class $classNameForLog",
                                                (processedClasses.toDouble() + innerTimeoutRatio) / totalClasses
                                            )
                                        }, 0, 500, TimeUnit.MILLISECONDS)
                                    try {
                                        val useEngine = when (model.projectType) {
                                            Spring -> when (model.springTestType) {
                                                UNIT_TEST -> true
                                                INTEGRATION_TEST -> false
                                            }
                                            else -> true
                                        }
                                        val useFuzzing = when (model.projectType) {
                                            Spring -> when (model.springTestType) {
                                                UNIT_TEST -> UtSettings.useFuzzing
                                                INTEGRATION_TEST -> true
                                            }

                                            else -> UtSettings.useFuzzing
                                        }
                                        val rdGenerateResult = process.generate(
                                            conflictTriggers = model.conflictTriggers,
                                            methods = methods,
                                            mockStrategyApi = model.mockStrategy,
                                            chosenClassesToMockAlways = model.chosenClassesToMockAlways,
                                            timeout = model.timeout,
                                            generationTimeout = model.timeout,
                                            isSymbolicEngineEnabled = useEngine,
                                            isFuzzingEnabled = useFuzzing,
                                            fuzzingValue = project.service<Settings>().fuzzingValue,
                                            searchDirectory = searchDirectory.pathString,
                                            taintConfigPath = taintConfigPath?.pathString
                                        )

                                        if (rdGenerateResult.notEmptyCases == 0) {
                                            if (!indicator.isCanceled) {
                                                if (model.srcClasses.size > 1) {
                                                    logger.error { "Failed to generate any tests cases for class $classNameForLog" }
                                                } else {
                                                    showErrorDialogLater(
                                                        model.project,
                                                        errorMessage(classNameForLog, secondsTimeout),
                                                        title = "Failed to generate unit tests for class $classNameForLog"
                                                    )
                                                }
                                            } else {
                                                logger.warn { "Generation was cancelled for class $classNameForLog" }
                                            }
                                        } else {
                                            testSetsByClass[srcClass] = rdGenerateResult
                                        }
                                    } finally {
                                        timerHandler.cancel(true)
                                    }
                                }
                                processedClasses++
                            }

                            if (processedClasses == 0) {
                                invokeLater {
                                    Messages.showInfoMessage(
                                        model.project,
                                        "No methods for test generation were found among selected items",
                                        "No Methods Found"
                                    )
                                }
                                return
                            }
                            updateIndicator(indicator, ProgressRange.CODEGEN, "Generate code for tests", 0.0)
                            // Commented out to generate tests for collected executions even if action was canceled.
                            // indicator.checkCanceled()

                            invokeLater {
                                generateTests(model, testSetsByClass, psi2KClass, process, indicator)
                                logger.info { "Generation complete" }
                            }
                        }
                    } finally {
                        LockFile.unlock()
                    }
                }
            }).queue()
        }
    }

    /**
     * Returns "{project}/.idea/utbot-taint-config.yaml" or null if it does not exists
     */
    private fun getTaintConfigPath(project: Project): Path? {
        val path = project.stateStore.directoryStorePath?.resolve("utbot-taint-config.yaml")
        return if (path != null && path.toFile().exists()) path else null
    }

    private fun clarifyBeanDefinitionReturnTypes(beanDefinitions: List<BeanDefinitionData>, project: Project) =
        beanDefinitions.map { bean ->
            // Here we extract a real return type.
            // E.g. for a method
            // public Toy getToy() { return new SpecToy() }
            // we want not Toy but SpecToy type.
            // If there are more than one return type, we take type from a signature.
            // We process beans with present additional data only.
            val beanType = runReadAction {
                val additionalData = bean.additionalData ?: return@runReadAction null

                val configPsiClass =
                    PsiClassHelper
                        .findClass(additionalData.configClassName, project)
                        ?: return@runReadAction null
                            .also {
                                logger.warn("Cannot find configuration class ${additionalData.configClassName}.")
                            }

                val beanPsiMethod =
                    configPsiClass
                        .findMethodsByName(bean.beanName)
                        .mapNotNull { jvmMethod ->
                            (jvmMethod as PsiMethod)
                                .takeIf { method ->
                                    !method.isAbstract && method.body?.isEmpty == false &&
                                            method.parameterList.parameters.map { it.type.canonicalText } == additionalData.parameterTypes
                                }
                        }
                        // Here we try to take a single element
                        // because we expect no or one method matching previous conditions only.
                        // If there were two or more similar methods in one class, it would be a weird case.
                        .singleOrNull()
                        ?: return@runReadAction null
                            .also {
                                logger.warn(
                                    "Several similar methods named ${bean.beanName} " +
                                            "were found in ${additionalData.configClassName} configuration class."
                                )
                            }

                val beanTypes =
                    PsiUtil
                        .findReturnStatements(beanPsiMethod)
                        .mapNotNullTo(mutableSetOf()) { stmt -> stmt.returnValue?.type?.canonicalText }

                beanTypes.singleOrNull() ?: bean.beanTypeName
            } ?: return@map bean

            BeanDefinitionData(
                beanName = bean.beanName,
                beanTypeName = beanType,
                additionalData = bean.additionalData
            )
        }

    private fun errorMessage(className: String?, timeout: Long) = buildString {
        appendLine("UnitTestBot failed to generate any test cases for class $className.")
        appendLine()
        appendLine("Try to alter test generation configuration, e.g. enable mocking and static mocking.")
        appendLine("Alternatively, you could try to increase current timeout $timeout sec for generating tests in generation dialog.")
    }

    private fun findSrcModule(srcClasses: Set<PsiClass>): Module {
        val srcModules = srcClasses.mapNotNull { it.module }.distinct()
        return when (srcModules.size) {
            0 -> error("Module for source classes not found")
            1 -> srcModules.first()
            else -> error("Can not generate tests for classes from different modules")
        }
    }

    private fun findPaths(modules: List<Module>): BuildPaths? {
        val buildDirs = CompilerPaths.getOutputPaths(modules.distinct().toTypedArray())
            .toList()
            .filter { Paths.get(it).exists() }
            .nullize() ?: return null

        val pathsList = PathsList()

        modules
            .distinct()
            .map { module -> OrderEnumerator.orderEntries(module).recursively().pathsList }
            .forEach { pathsList.addAll(it.pathList) }

        val (classpath, classpathList) = if (IntelliJApiHelper.isAndroidStudio()) {
            // Filter out manifests from classpath.
            val filterPredicate = { it: String ->
                !it.contains("manifest", ignoreCase = true)
            }
            val classpathList = pathsList.pathList.filter(filterPredicate)
            val classpath = StringUtil.join(classpathList, File.pathSeparator)
            Pair(classpath, classpathList)
        } else {
            val classpath = pathsList.pathsString
            val classpathList = pathsList.pathList
            Pair(classpath, classpathList)
        }
        val pluginJarsPath = Paths.get(PathManager.getPluginsPath(), "utbot-intellij", "lib").toFile().listFiles()
            ?: error("Can't find plugin folder.")
        return BuildPaths(buildDirs, classpath, classpathList, pluginJarsPath.map { it.path })
    }

    data class BuildPaths(
        val buildDirs: List<String>,
        val classpath: String,
        val classpathList: List<String>,
        val pluginJarsPath: List<String>
        // ^ TODO: Now we collect ALL dependent libs and pass them to the instrumented and spring analyzer processes. Most of them are redundant.
    )
}