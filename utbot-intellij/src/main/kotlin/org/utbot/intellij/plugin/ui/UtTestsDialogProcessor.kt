package org.utbot.intellij.plugin.ui

import org.utbot.framework.JdkPathService
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withSubstitutionCondition
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.intellij.plugin.generator.CodeGenerator
import org.utbot.intellij.plugin.generator.TestGenerator.generateTests
import org.utbot.intellij.plugin.generator.findMethodsInClassMatchingSelected
import org.utbot.intellij.plugin.ui.utils.jdkVersion
import org.utbot.intellij.plugin.ui.utils.testModule
import org.utbot.intellij.plugin.util.AndroidApiHelper
import org.utbot.intellij.plugin.util.PluginJdkPathProvider
import com.intellij.compiler.impl.CompositeScope
import com.intellij.compiler.impl.OneProjectItemCompileScope
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.util.module
import org.utbot.engine.util.mockListeners.ForceMockListener
import org.utbot.engine.util.mockListeners.ForceStaticMockListener
import org.utbot.intellij.plugin.error.showErrorDialogLater

object UtTestsDialogProcessor {

    private val logger = KotlinLogging.logger {}

    fun createDialogAndGenerateTests(
        project: Project,
        srcClasses: Set<PsiClass>,
        focusedMethod: MemberInfo?,
    ) {
        createDialog(project, srcClasses, focusedMethod)?.let {
            if (it.showAndGet()) createTests(project, it.model)
        }
    }

    private fun createDialog(
        project: Project,
        srcClasses: Set<PsiClass>,
        focusedMethod: MemberInfo?,
    ): GenerateTestsDialogWindow? {
        val srcModule = findSrcModule(srcClasses)
        val testModule = srcModule.testModule(project)

        JdkPathService.jdkPathProvider = PluginJdkPathProvider(project, testModule)
        val jdkVersion = try {
            testModule.jdkVersion()
        } catch (e: IllegalStateException) {
            // Just ignore it here, notification will be shown in
            // org.utbot.intellij.plugin.ui.utils.ModuleUtilsKt.jdkVersionBy
            return null
        }

        return GenerateTestsDialogWindow(
            GenerateTestsModel(
                project,
                srcModule,
                testModule,
                jdkVersion,
                srcClasses,
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
                    model.srcClasses.map{ OneProjectItemCompileScope(project, it.containingFile.virtualFile) }.toTypedArray()
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

                            val testCasesByClass = mutableMapOf<PsiClass, List<UtTestCase>>()
                            var processedClasses = 0
                            val totalClasses = model.srcClasses.size

                            for (srcClass in model.srcClasses) {
                                val methods = ReadAction.nonBlocking<List<UtMethod<*>>> {
                                    val clazz = classLoader.loadClass(srcClass.qualifiedName).kotlin
                                    val srcMethods = model.selectedMethods?.toList() ?:
                                        TestIntegrationUtils.extractClassMethods(srcClass, false)
                                    findMethodsInClassMatchingSelected(clazz, srcMethods)
                                }.executeSynchronously()

                                val className = srcClass.name
                                if (methods.isEmpty()) {
                                    logger.error { "No methods matching selected found in class $className." }
                                    continue
                                }

                                indicator.text = "Generate test cases for class $className"
                                if (totalClasses > 1) {
                                    indicator.fraction = indicator.fraction.coerceAtLeast(0.9 * processedClasses / totalClasses)
                                }

                                //we should not substitute statics for parametrized tests
                                val shouldSubstituteStatics =
                                    model.parametrizedTestSource != ParametrizedTestSource.PARAMETRIZE
                                // set timeout for concrete execution and for generated tests
                                UtSettings.concreteExecutionTimeoutInChildProcess = model.hangingTestsTimeout.timeoutMs

                                val searchDirectory =  ReadAction
                                    .nonBlocking<Path> { project.basePath?.let { Paths.get(it) } ?: Paths.get(srcClass.containingFile.virtualFile.parent.path) }
                                    .executeSynchronously()

                                withSubstitutionCondition(shouldSubstituteStatics) {
                                    val mockFrameworkInstalled = model.mockFramework?.isInstalled ?: true
                                    val codeGenerator = CodeGenerator(
                                        searchDirectory = searchDirectory,
                                        mockStrategy = model.mockStrategy,
                                        project = model.project,
                                        buildDir = buildDir,
                                        classpath = classpath,
                                        pluginJarsPath = pluginJarsPath.joinToString(separator = File.pathSeparator),
                                        chosenClassesToMockAlways = model.chosenClassesToMockAlways
                                    ) { indicator.isCanceled }

                                    val forceMockListener = if (!mockFrameworkInstalled) {
                                         ForceMockListener().apply {
                                             codeGenerator.generator.engineActions.add { engine -> engine.attachMockListener(this) }
                                         }
                                    } else {
                                        null
                                    }

                                    val forceStaticMockListener = if (!model.staticsMocking.isConfigured) {
                                        ForceStaticMockListener().apply {
                                            codeGenerator.generator.engineActions.add { engine -> engine.attachMockListener(this) }
                                        }
                                    } else {
                                        null
                                    }

                                    val notEmptyCases = withUtContext(context) {
                                        codeGenerator
                                            .generateForSeveralMethods(methods, model.timeout)
                                            .filterNot { it.executions.isEmpty() && it.errors.isEmpty() }
                                    }

                                    if (notEmptyCases.isEmpty()) {
                                        showErrorDialogLater(
                                            model.project,
                                            errorMessage(className, secondsTimeout),
                                            title = "Failed to generate unit tests for class $className"
                                        )
                                    } else {
                                        testCasesByClass[srcClass] = notEmptyCases
                                    }

                                    forceMockListener?.run {
                                        model.forceMockHappened = forceMockHappened
                                    }

                                    forceStaticMockListener?.run {
                                        model.forceStaticMockHappened = forceStaticMockHappened
                                    }

                                    timerHandler.cancel(true)
                                }
                                processedClasses++
                            }

                            indicator.fraction = indicator.fraction.coerceAtLeast(0.9)
                            indicator.text = "Generate code for tests"
                            // Commented out to generate tests for collected executions even if action was canceled.
                            // indicator.checkCanceled()

                            invokeLater {
                                withUtContext(context) {
                                    generateTests(model, testCasesByClass)
                                }
                            }
                        }
                    }).queue()
                }
            }
    }

    private fun errorMessage(className: String?, timeout: Long) = buildString {
        appendLine("UtBot failed to generate any test cases for class $className.")
        appendLine()
        appendLine("Try to alter test generation configuration, e.g. enable mocking and static mocking.")
        appendLine("Alternatively, you could try to increase current timeout $timeout sec for generating tests in generation dialog.")
    }
}


internal fun urlClassLoader(classpath: List<String>) =
    URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray())

fun findSrcModule(srcClasses: Set<PsiClass>): Module {
    val srcModules = srcClasses.mapNotNull { it.module }.distinct()
    return when (srcModules.size) {
        0 -> error("Module for source classes not found")
        1 -> srcModules.first()
        else -> error("Can not generate tests for classes from different modules")
    }
}

internal fun findPaths(srcClasses: Set<PsiClass>): BuildPaths? {
    val srcModule = findSrcModule(srcClasses)
    val buildDir = CompilerPaths.getModuleOutputPath(srcModule, false) ?: return null
    val pathsList = OrderEnumerator.orderEntries(srcModule).recursively().pathsList

    val (classpath, classpathList) = if (AndroidApiHelper.isAndroidStudio()) {
        // Add $JAVA_HOME/jre/lib/rt.jar to path.
        // This allows Soot to analyze real java instead of stub version in Android SDK on local machine.
        pathsList.add(
            System.getenv("JAVA_HOME") + File.separator + Paths.get("jre", "lib", "rt.jar")
        )

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