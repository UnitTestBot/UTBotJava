package org.utbot.intellij.plugin.language.python

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyClass
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.utbot.common.PathUtil.toPath
import org.utbot.common.appendHtmlLine
import org.utbot.framework.UtSettings
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.WarningTestsReportNotifier
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.python.code.PythonCode
import org.utbot.python.code.PythonCode.Companion.getFromString
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration
import org.utbot.python.framework.codegen.PythonCodeLanguage
import org.utbot.python.utils.RequirementsUtils.installRequirements
import org.utbot.python.utils.RequirementsUtils.requirements
import java.io.File
import kotlin.io.path.Path

const val DEFAULT_TIMEOUT_FOR_RUN_IN_MILLIS = 2000L

object PythonDialogProcessor {
    fun createDialogAndGenerateTests(
        project: Project,
        functionsToShow: Set<PyFunction>,
        containingClass: PyClass?,
        focusedMethod: PyFunction?,
        file: PyFile
    ) {
        val dialog = createDialog(project, functionsToShow, containingClass, focusedMethod, file)
        if (!dialog.showAndGet()) {
            return
        }

        createTests(project, dialog.model)
    }

    private fun createDialog(
        project: Project,
        functionsToShow: Set<PyFunction>,
        containingClass: PyClass?,
        focusedMethod: PyFunction?,
        file: PyFile
    ): PythonDialogWindow {
        val srcModule = findSrcModule(functionsToShow)
        val testModules = srcModule.testModules(project)
        val (directoriesForSysPath, moduleToImport) = getDirectoriesForSysPath(srcModule, file)

        return PythonDialogWindow(
            PythonTestsModel(
                project,
                srcModule,
                testModules,
                functionsToShow,
                containingClass,
                if (focusedMethod != null) setOf(focusedMethod) else null,
                file,
                directoriesForSysPath,
                moduleToImport,
                UtSettings.utBotGenerationTimeoutInMillis,
                DEFAULT_TIMEOUT_FOR_RUN_IN_MILLIS,
                visitOnlySpecifiedSource = false,
                codeGenLanguage = PythonCodeLanguage,
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestsModel): List<PythonMethod>? {
        val code = getPyCodeFromPyFile(model.file, model.currentPythonModule) ?: return null

        val shownFunctions: Set<PythonMethod> =
            if (model.containingClass == null) {
                code.getToplevelFunctions().toSet()
            } else {
                val classes = code.getToplevelClasses()
                val myClass = classes.find { it.name == model.containingClass.name }
                    ?: error("Didn't find containing class")
                myClass.methods.toSet()
            }

        return model.selectedFunctions.map { pyFunction ->
            shownFunctions.find { pythonMethod ->
                pythonMethod.name == pyFunction.name
            } ?: error("Didn't find PythonMethod ${pyFunction.name}")
        }
    }

    private fun getOutputFileName(model: PythonTestsModel) =
        "test_${model.currentPythonModule.camelToSnakeCase().replace('.', '_')}.py"

    private fun createTests(project: Project, model: PythonTestsModel) {
        ProgressManager.getInstance().run(object : Backgroundable(project, "Generate python tests") {
            override fun run(indicator: ProgressIndicator) {
                val pythonPath = model.srcModule.sdk?.homePath
                if (pythonPath == null) {
                    showErrorDialogLater(
                        project,
                        message = "Couldn't find Python interpreter",
                        title = "Python test generation error"
                    )
                    return
                }
                val methods = findSelectedPythonMethods(model)
                if (methods == null) {
                    showErrorDialogLater(
                        project,
                        message = "Couldn't parse file. Maybe it contains syntax error?",
                        title = "Python test generation error"
                    )
                    return
                }
                val testSourceRootPath = model.testSourceRoot!!.path
                processTestGeneration(
                    pythonPath = pythonPath,
                    testSourceRoot = testSourceRootPath,
                    pythonFilePath = model.file.virtualFile.path,
                    pythonFileContent = getContentFromPyFile(model.file),
                    directoriesForSysPath = model.directoriesForSysPath,
                    currentPythonModule = model.currentPythonModule,
                    pythonMethods = methods,
                    containingClassName = model.containingClass?.name,
                    timeout = model.timeout,
                    testFramework = model.testFramework,
                    timeoutForRun = model.timeoutForRun,
                    visitOnlySpecifiedSource = model.visitOnlySpecifiedSource,
                    isCanceled = { indicator.isCanceled },
                    checkingRequirementsAction = { indicator.text = "Checking requirements" },
                    requirementsAreNotInstalledAction = {
                        askAndInstallRequirementsLater(model.project, pythonPath)
                        PythonTestGenerationProcessor.MissingRequirementsActionResult.NOT_INSTALLED
                    },
                    startedLoadingPythonTypesAction = { indicator.text = "Loading information about Python types" },
                    startedTestGenerationAction = { indicator.text = "Generating tests" },
                    notGeneratedTestsAction = {
                        showErrorDialogLater(
                            project,
                            message = "Cannot create tests for the following functions: " + it.joinToString(),
                            title = "Python test generation error"
                        )
                    },
                    writeTestTextToFile = { generatedCode ->
                        invokeLater {
                            runWriteAction {
                                val testDirAsVirtualFile =
                                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(testSourceRootPath))
                                val testDir = PsiDirectoryFactory.getInstance(project).createDirectory(
                                    testDirAsVirtualFile!!
                                )
                                val testFileName = getOutputFileName(model)
                                val testPsiFile = PsiFileFactory.getInstance(project)
                                    .createFileFromText(testFileName, PythonLanguageAssistant.language, generatedCode)
                                testDir.findFile(testPsiFile.name)?.delete()
                                testDir.add(testPsiFile)
                                val file = testDir.findFile(testPsiFile.name)!!
                                CodeInsightUtil.positionCursor(project, file, file)
                            }
                        }
                    },
                    processMypyWarnings = {
                        val message = it.fold(StringBuilder()) { acc, line -> acc.appendHtmlLine(line) }
                        WarningTestsReportNotifier.notify(message.toString())
                    },
                    startedCleaningAction = { indicator.text = "Cleaning up..." },
                    pythonRunRoot = Path(model.testSourceRoot!!.path)
                )
            }
        })
    }

    private fun askAndInstallRequirementsLater(project: Project, pythonPath: String) {
        val message = """
            Some requirements are not installed.
            Requirements: <br>
            ${requirements.joinToString("<br>")}
            <br>
            Install them?
        """.trimIndent()
        invokeLater {
            val result = Messages.showYesNoDialog(
                project,
                message,
                "Requirements Error",
                null,
                null
            )
            if (result == Messages.NO)
                return@invokeLater

            ProgressManager.getInstance().run(object : Backgroundable(project, "Installing requirements") {
                override fun run(indicator: ProgressIndicator) {
                    val installResult = installRequirements(pythonPath)

                    if (installResult.exitValue != 0) {
                        showErrorDialogLater(
                            project,
                            "Requirements installing failed",
                            "Requirements error"
                        )
                    }
                }
            })
        }
    }
}

fun findSrcModule(functions: Collection<PyFunction>): Module {
    val srcModules = functions.mapNotNull { it.module }.distinct()
    return when (srcModules.size) {
        0 -> error("Module for source classes not found")
        1 -> srcModules.first()
        else -> error("Can not generate tests for classes from different modules")
    }
}

fun getContentFromPyFile(file: PyFile) = file.viewProvider.contents.toString()

fun getPyCodeFromPyFile(file: PyFile, pythonModule: String): PythonCode? {
    val content = getContentFromPyFile(file)
    return getFromString(content, file.virtualFile.path, pythonModule = pythonModule)
}

fun getDirectoriesForSysPath(
    srcModule: Module,
    file: PyFile
): Pair<Set<String>, String> {
    val sources = ModuleRootManager.getInstance(srcModule).getSourceRoots(false).toMutableList()
    val ancestor = ProjectFileIndex.SERVICE.getInstance(file.project).getContentRootForFile(file.virtualFile)
    if (ancestor != null && !sources.contains(ancestor))
        sources.add(ancestor)

    var importPath = ancestor?.let { VfsUtil.getParentDir(VfsUtilCore.getRelativeLocation(file.virtualFile, it)) } ?: ""
    if (importPath != "")
        importPath += "."

    return Pair(
        sources.map { it.path }.toSet(),
        "${importPath}${file.name}".removeSuffix(".py").toPath().joinToString(".").replace("/", File.separator)
    )
}