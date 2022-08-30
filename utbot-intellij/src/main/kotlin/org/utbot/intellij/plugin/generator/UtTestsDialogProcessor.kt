package org.utbot.intellij.plugin.generator

import com.intellij.compiler.impl.CompositeScope
import com.intellij.compiler.impl.OneProjectItemCompileScope
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerManager
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
import com.intellij.util.concurrency.AppExecutorUtil
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.util.module
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.Predictors
import org.utbot.common.allNestedClasses
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.testFlow
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withStaticsSubstitutionRequired
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.intellij.plugin.generator.CodeGenerationController.generateTests
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.intellij.plugin.util.IntelliJApiHelper
import org.utbot.intellij.plugin.util.PluginJdkInfoProvider
import org.utbot.intellij.plugin.util.signature
import org.utbot.summary.summarize
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.framework.PathSelectorType
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.util.extractClassMethodsIncludingNested
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.util.PluginWorkingDirProvider
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

object UtTestsDialogProcessor {

    private val logger = KotlinLogging.logger {}

    fun createDialogAndGenerateTests(
        project: Project,
        srcClasses: Set<PsiClass>,
        extractMembersFromSrcClasses: Boolean,
        focusedMethod: MemberInfo?,
    ) {
        createDialog(project, srcClasses, extractMembersFromSrcClasses, focusedMethod)?.let {
            if (it.showAndGet()) createTests(project, it.model)
        }
    }

    private fun createDialog(
        project: Project,
        srcClasses: Set<PsiClass>,
        extractMembersFromSrcClasses: Boolean,
        focusedMethod: MemberInfo?,
    ): GenerateTestsDialogWindow? {
        val srcModule = findSrcModule(srcClasses)
        val testModules = srcModule.testModules(project)

        JdkInfoService.jdkInfoProvider = PluginJdkInfoProvider(project)
        // we want to start the child process in the same directory as the test runner
        WorkingDirService.workingDirProvider = PluginWorkingDirProvider(project)

        if (project.isBuildWithGradle && testModules.flatMap { it.suitableTestSourceRoots() }.isEmpty()) {
            val errorMessage = """
                <html>No test source roots found in the project.<br>
                Please, <a href="https://www.jetbrains.com/help/idea/testing.html#add-test-root">create or configure</a> at least one test source root.
            """.trimIndent()
            showErrorDialogLater(project, errorMessage, "Test source roots not found")
            return null
        }

        return GenerateTestsDialogWindow(
            GenerateTestsModel(
                project,
                srcModule,
                testModules,
                srcClasses,
                extractMembersFromSrcClasses,
                if (focusedMethod != null) setOf(focusedMethod) else null,
                UtSettings.utBotGenerationTimeoutInMillis,
            )
        )
    }

    private fun createTests(project: Project, model: GenerateTestsModel) {
        CompilerManager.getInstance(project)
            .make(
                // Compile only chosen classes and their dependencies before generation.
                CompositeScope(
                    model.srcClasses.map { OneProjectItemCompileScope(project, it.containingFile.virtualFile) }
                        .toTypedArray()
                )
            ) { aborted: Boolean, errors: Int, _: Int, _: CompileContext ->
                if (!aborted && errors == 0) {
                    (object : Task.Backgroundable(project, "Generate tests") {

                        override fun run(indicator: ProgressIndicator) {
                            val startTime = System.currentTimeMillis()
                            val secondsTimeout = TimeUnit.MILLISECONDS.toSeconds(model.timeout)
                            val totalTimeout = model.timeout * model.srcClasses.size

                            indicator.isIndeterminate = false
                            indicator.text = "Generate tests: read classes"

                            val timerHandler = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
                                indicator.fraction = (System.currentTimeMillis() - startTime).toDouble() / totalTimeout
                            }, 0, 500, TimeUnit.MILLISECONDS)

                            val buildPaths = ReadAction
                                .nonBlocking<BuildPaths?> { findPaths(model.srcClasses) }
                                .executeSynchronously()
                                ?: return

                            val (buildDir, classpath, classpathList, pluginJarsPath) = buildPaths
                            val classLoader = urlClassLoader(listOf(buildDir) + classpathList)
                            val context = UtContext(classLoader)

                            val testSetsByClass = mutableMapOf<PsiClass, List<UtMethodTestSet>>()
                            val psi2KClass = mutableMapOf<PsiClass, KClass<*>>()
                            var processedClasses = 0
                            val totalClasses = model.srcClasses.size

                            configureML()

                            val testCaseGenerator = TestCaseGenerator(
                                Paths.get(buildDir),
                                classpath,
                                pluginJarsPath.joinToString(separator = File.pathSeparator),
                                isCanceled = { indicator.isCanceled }
                            )

                            for (srcClass in model.srcClasses) {
                                val methods = ReadAction.nonBlocking<List<UtMethod<*>>> {
                                    val canonicalName = srcClass.canonicalName
                                    val clazz = classLoader.loadClass(canonicalName).kotlin
                                    psi2KClass[srcClass] = clazz

                                    val srcMethods = if (model.extractMembersFromSrcClasses) {
                                        val chosenMethods = model.selectedMembers?.filter { it.member is PsiMethod } ?: listOf()
                                        val chosenNestedClasses = model.selectedMembers?.mapNotNull { it.member as? PsiClass } ?: listOf()
                                        chosenMethods + chosenNestedClasses.flatMap {
                                            it.extractClassMethodsIncludingNested(false)
                                        }
                                    } else {
                                        srcClass.extractClassMethodsIncludingNested(false)
                                    }
                                    DumbService.getInstance(project).runReadActionInSmartMode(Computable {
                                        clazz.allNestedClasses.flatMap {
                                            findMethodsInClassMatchingSelected(it, srcMethods)
                                        }
                                    })
                                }.executeSynchronously()

                                val className = srcClass.name
                                if (methods.isEmpty()) {
                                    logger.error { "No methods matching selected found in class $className." }
                                    continue
                                }

                                indicator.text = "Generate test cases for class $className"
                                if (totalClasses > 1) {
                                    indicator.fraction =
                                        indicator.fraction.coerceAtLeast(0.9 * processedClasses / totalClasses)
                                }

                                // set timeout for concrete execution and for generated tests
                                UtSettings.concreteExecutionTimeoutInChildProcess = model.hangingTestsTimeout.timeoutMs

                                val searchDirectory = ReadAction
                                    .nonBlocking<Path> {
                                        project.basePath?.let { Paths.get(it) }
                                            ?: Paths.get(srcClass.containingFile.virtualFile.parent.path)
                                    }
                                    .executeSynchronously()

                                withStaticsSubstitutionRequired(true) {
                                    val mockFrameworkInstalled = model.mockFramework?.isInstalled ?: true

                                    if (!mockFrameworkInstalled) {
                                        ForceMockListener.create(testCaseGenerator, model.conflictTriggers)
                                    }

                                    if (!model.staticsMocking.isConfigured) {
                                        ForceStaticMockListener.create(testCaseGenerator, model.conflictTriggers)
                                    }

                                    val notEmptyCases = runCatching {
                                        withUtContext(context) {
                                            testCaseGenerator
                                                .generate(
                                                    methods,
                                                    model.mockStrategy,
                                                    model.chosenClassesToMockAlways,
                                                    model.timeout,
                                                    generate = testFlow {
                                                        generationTimeout = model.timeout
                                                        isSymbolicEngineEnabled = true
                                                        isFuzzingEnabled = UtSettings.useFuzzing
                                                        fuzzingValue = project.service<Settings>().fuzzingValue
                                                    }
                                                )
                                                .map { it.summarize(searchDirectory) }
                                                .filterNot { it.executions.isEmpty() && it.errors.isEmpty() }
                                        }
                                    }.getOrDefault(listOf())

                                    if (notEmptyCases.isEmpty()) {
                                        if (model.srcClasses.size > 1) {
                                            logger.error { "Failed to generate any tests cases for class $className" }
                                        } else {
                                            showErrorDialogLater(
                                                model.project,
                                                errorMessage(className, secondsTimeout),
                                                title = "Failed to generate unit tests for class $className"
                                            )
                                        }
                                    } else {
                                        testSetsByClass[srcClass] = notEmptyCases
                                    }

                                    timerHandler.cancel(true)
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

                            indicator.fraction = indicator.fraction.coerceAtLeast(0.9)
                            indicator.text = "Generate code for tests"
                            // Commented out to generate tests for collected executions even if action was canceled.
                            // indicator.checkCanceled()

                            invokeLater {
                                withUtContext(context) {
                                    generateTests(model, testSetsByClass, psi2KClass)
                                }
                            }
                        }
                    }).queue()
                }
            }
    }

    private val PsiClass.canonicalName: String
        get() {
            return if (packageName.isEmpty()) {
                qualifiedName?.replace(".", "$") ?: ""
            } else {
                val name = qualifiedName
                    ?.substringAfter("$packageName.")
                    ?.replace(".", "$")
                    ?: ""
                "$packageName.$name"
            }
        }

    /**
     * Configures utbot-analytics models for the better path selection.
     *
     * NOTE: If analytics configuration for the NN Path Selector could not be loaded,
     * it switches to the [PathSelectorType.INHERITORS_SELECTOR].
     */
    private fun configureML() {
        logger.info { "PathSelectorType: ${UtSettings.pathSelectorType}" }

        if (UtSettings.pathSelectorType == PathSelectorType.NN_REWARD_GUIDED_SELECTOR) {
            val analyticsConfigurationClassPath = UtSettings.analyticsConfigurationClassPath
            try {
                Class.forName(analyticsConfigurationClassPath)
                Predictors.stateRewardPredictor = EngineAnalyticsContext.stateRewardPredictorFactory()

                logger.info { "RewardModelPath: ${UtSettings.rewardModelPath}" }
            } catch (e: ClassNotFoundException) {
                logger.error {
                    "Configuration of the predictors from the utbot-analytics module described in the class: " +
                            "$analyticsConfigurationClassPath is not found!"
                }

                logger.info(e) {
                    "Error while initialization of ${UtSettings.pathSelectorType}. Changing pathSelectorType on INHERITORS_SELECTOR"
                }
                UtSettings.pathSelectorType = PathSelectorType.INHERITORS_SELECTOR
            }
        }
    }

    private fun errorMessage(className: String?, timeout: Long) = buildString {
        appendLine("UtBot failed to generate any test cases for class $className.")
        appendLine()
        appendLine("Try to alter test generation configuration, e.g. enable mocking and static mocking.")
        appendLine("Alternatively, you could try to increase current timeout $timeout sec for generating tests in generation dialog.")
    }

    private fun findMethodsInClassMatchingSelected(
        clazz: KClass<*>,
        selectedMethods: List<MemberInfo>
    ): List<UtMethod<*>> {
        val selectedSignatures = selectedMethods.map { it.signature() }
        return clazz.functions
            .sortedWith(compareBy { selectedSignatures.indexOf(it.signature()) })
            .filter { it.signature().normalized() in selectedSignatures }
            .map { UtMethod(it, clazz) }
    }

    private fun urlClassLoader(classpath: List<String>) =
        URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray())

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
        val buildDir = CompilerPaths.getModuleOutputPath(srcModule, false) ?: return null
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
        return BuildPaths(buildDir, classpath, classpathList, pluginJarsPath.map { it.path })
    }

    data class BuildPaths(
        val buildDir: String,
        val classpath: String,
        val classpathList: List<String>,
        val pluginJarsPath: List<String>
        // ^ TODO: Now we collect ALL dependent libs and pass them to the child process. Most of them are redundant.
    )
}
