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
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.task.ProjectTaskManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.containers.nullize
import com.intellij.util.io.exists
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.util.module
import org.utbot.framework.CancellationStrategyType.CANCEL_EVERYTHING
import org.utbot.framework.CancellationStrategyType.NONE
import org.utbot.framework.CancellationStrategyType.SAVE_PROCESSED_RESULTS
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.JavaDocCommentStyle
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.framework.plugin.api.util.withStaticsSubstitutionRequired
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.intellij.plugin.generator.CodeGenerationController.generateTests
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.intellij.plugin.process.RdTestGenerationResult
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.intellij.plugin.util.IntelliJApiHelper
import org.utbot.intellij.plugin.util.PluginJdkInfoProvider
import org.utbot.intellij.plugin.util.PluginWorkingDirProvider
import org.utbot.intellij.plugin.util.assertIsNonDispatchThread
import org.utbot.intellij.plugin.util.extractClassMethodsIncludingNested
import org.utbot.rd.terminateOnException
import org.utbot.spring.beans.SpringBeansAnalyzer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object UtTestsDialogProcessor {
    private val logger = KotlinLogging.logger {}

    enum class ProgressRange(val from : Double, val to: Double) {
        SOLVING(from = 0.0, to = 0.9),
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
            UtSettings.utBotGenerationTimeoutInMillis,
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

    private fun createTests(project: Project, model: GenerateTestsModel) {
        val promise = ProjectTaskManager.getInstance(project).compile(
            // Compile only chosen classes and their dependencies before generation.
            *model.srcClasses.map { it.containingFile.virtualFile }.toTypedArray()
        )
        promise.onSuccess {
            if (it.hasErrors() || it.isAborted)
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
                        val tempDirectory = Files.createTempDirectory(
                                Paths.get("D:\\Projects\\spring-starter-lesson-28\\build"),
                            "utbot-spring-analyzer-"
                            ).toFile()
                        val springAnalysisProcess = ProcessBuilder().directory(tempDirectory)
                            .command(
                                "java",
                                "-jar",
                                "C:\\UtBot\\UTBotJava\\utbot-spring-analyzer\\build\\libs\\utbot-spring-analyzer-2023.02-SNAPSHOT.jar",
                                "D:\\Projects\\spring-starter-lesson-28\\build\\classes\\java\\main",
                                "com.dmdev.spring.config.ApplicationConfiguration"
                            ).start()
                        springAnalysisProcess.waitFor()

                        val beansFile = tempDirectory.resolve("SpringBeans.txt")
                        println("Beans have been written to file ${beansFile.toPath()}")


                        logger.info { "Collecting information phase started at ${now()}" }
                        val secondsTimeout = TimeUnit.MILLISECONDS.toSeconds(model.timeout)

                        indicator.isIndeterminate = false
                        updateIndicator(indicator, ProgressRange.SOLVING, "Generate tests: read classes", 0.0)

                        val buildPaths = ReadAction
                            .nonBlocking<BuildPaths?> { findPaths(model.srcClasses) }
                            .executeSynchronously()
                            ?: return

                        val (buildDirs, classpath, classpathList, pluginJarsPath) = buildPaths

                        val testSetsByClass = mutableMapOf<PsiClass, RdTestGenerationResult>()
                        val psi2KClass = mutableMapOf<PsiClass, ClassId>()
                        var processedClasses = 0
                        val totalClasses = model.srcClasses.size
                        val classNameToPath = runReadAction {
                            model.srcClasses.map { psiClass ->
                                psiClass.canonicalName to psiClass.containingFile.virtualFile.canonicalPath
                            }.toMap()
                        }
                        val process = EngineProcess.createBlocking(project, classNameToPath)

                        process.terminateOnException { _ ->
                            process.setupUtContext(buildDirs + classpathList)
                            process.createTestGenerator(
                                buildDirs,
                                classpath,
                                pluginJarsPath.joinToString(separator = File.pathSeparator),
                                JdkInfoService.provide()
                            ) {
                                ApplicationManager.getApplication().runReadAction(Computable {
                                    indicator.isCanceled
                                })
                            }


                            for (srcClass in model.srcClasses) {
                                if (indicator.isCanceled) {
                                    when (UtSettings.cancellationStrategyType) {
                                        NONE -> {}
                                        SAVE_PROCESSED_RESULTS,
                                        CANCEL_EVERYTHING -> break
                                    }
                                }

                                val (methods, classNameForLog) = process.executeWithTimeoutSuspended {
                                    var canonicalName = ""
                                    var srcMethods: List<MemberInfo> = emptyList()
                                    var srcNameForLog: String? = null
                                    DumbService.getInstance(project)
                                        .runReadActionInSmartMode(Computable {
                                            canonicalName = srcClass.canonicalName
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
                                    val classId = process.obtainClassId(canonicalName)
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

                                withStaticsSubstitutionRequired(true) {
                                    val mockFrameworkInstalled = model.mockFramework?.isInstalled ?: true

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
                                        val rdGenerateResult = process.generate(
                                            mockFrameworkInstalled,
                                            model.staticsMocking.isConfigured,
                                            model.conflictTriggers,
                                            methods,
                                            model.mockStrategy,
                                            model.chosenClassesToMockAlways,
                                            model.timeout,
                                            model.timeout,
                                            true,
                                            UtSettings.useFuzzing,
                                            project.service<Settings>().fuzzingValue,
                                            searchDirectory.pathString
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
                                        "No methods found"
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

    private val PsiClass.canonicalName: String
    /*
    This method calculates exactly name that is used by compiler convention,
    i.e. result is the exact name of .class file for provided PsiClass.
    This value is used to provide classes to engine process - follow usages for clarification.
    Equivalent for Class.getCanonicalName.
    P.S. We cannot load project class in IDEA jvm
     */
        get() {
            return if (packageName.isEmpty()) {
                qualifiedName?.replace(".", "$") ?: ""
            } else {
                val name = qualifiedName
                    ?.substringAfter("$packageName.")
                    ?.replace(".", "$")
                    ?: error("Unable to get canonical name for $this")
                "$packageName.$name"
            }
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

    private fun findPaths(srcClasses: Set<PsiClass>): BuildPaths? {
        val srcModule = findSrcModule(srcClasses)

        val buildDirs = CompilerPaths.getOutputPaths(arrayOf(srcModule))
            .toList()
            .filter { Paths.get(it).exists() }
            .nullize() ?: return null

        val pathsList = OrderEnumerator.orderEntries(srcModule).recursively().pathsList

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
        // ^ TODO: Now we collect ALL dependent libs and pass them to the instrumented process. Most of them are redundant.
    )
}