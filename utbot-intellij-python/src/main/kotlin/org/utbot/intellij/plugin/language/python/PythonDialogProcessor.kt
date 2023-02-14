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
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.utbot.common.PathUtil.toPath
import org.utbot.common.appendHtmlLine
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.intellij.plugin.ui.WarningTestsReportNotifier
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.utils.RequirementsUtils.installRequirements
import org.utbot.python.utils.RequirementsUtils.requirements
import org.utbot.python.utils.camelToSnakeCase
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
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
                cgLanguageAssistant = PythonCgLanguageAssistant,
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestsModel): List<PythonMethod>? {
        val shownFunctions: Set<PythonMethod> =
            if (model.containingClass == null) {
                model.file.topLevelFunctions.mapNotNull { it.toPythonMethod() }.toSet()
            } else {
                val classes = model.file.topLevelClasses
                val myClass = classes.find { it.name == model.containingClass.name }
                    ?: error("Didn't find containing class")
                myClass.methods.mapNotNull { it.toPythonMethod() }.toSet()
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
                if (!LockFile.lock()) {
                    return
                }
                try {
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
                    processTestGeneration(
                        pythonPath = pythonPath,
                        pythonFilePath = model.file.virtualFile.path,
                        pythonFileContent = getContentFromPyFile(model.file),
                        directoriesForSysPath = model.directoriesForSysPath,
                        currentPythonModule = model.currentPythonModule,
                        pythonMethods = methods,
                        containingClassName = model.containingClass?.name,
                        timeout = model.timeout,
                        testFramework = model.testFramework,
                        timeoutForRun = model.timeoutForRun,
                        writeTestTextToFile = { generatedCode ->
                            writeGeneratedCodeToPsiDocument(generatedCode, model)
                        },
                        pythonRunRoot = Path(model.testSourceRootPath),
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
                        processMypyWarnings = {
                            val message = it.fold(StringBuilder()) { acc, line -> acc.appendHtmlLine(line) }
                            WarningTestsReportNotifier.notify(message.toString())
                        },
                        startedCleaningAction = { indicator.text = "Cleaning up..." }
                    )
                } finally {
                    LockFile.unlock()
                }
            }
        })
    }

    private fun getDirectoriesFromRoot(root: Path, path: Path): List<String> {
        if (path == root || path.parent == null)
            return emptyList()
        return getDirectoriesFromRoot(root, path.parent) + listOf(path.fileName.toString())
    }

    private fun createPsiDirectoryForTestSourceRoot(model: PythonTestsModel): PsiDirectory {
        val root = getContentRoot(model.project, model.file.virtualFile)
        val paths = getDirectoriesFromRoot(
            Paths.get(root.path),
            Paths.get(model.testSourceRootPath)
        )
        val rootPSI = getContainingElement<PsiDirectory>(model.file) { it.virtualFile == root }!!
        return paths.fold(rootPSI) { acc, folderName ->
            acc.findSubdirectory(folderName) ?: acc.createSubdirectory(folderName)
        }
    }

    private fun writeGeneratedCodeToPsiDocument(generatedCode: String, model: PythonTestsModel) {
        invokeLater {
            runWriteAction {
                val testDir = createPsiDirectoryForTestSourceRoot(model)
                val testFileName = getOutputFileName(model)
                val testPsiFile = PsiFileFactory.getInstance(model.project)
                    .createFileFromText(testFileName, PythonLanguageAssistant.language, generatedCode)
                testDir.findFile(testPsiFile.name)?.delete()
                testDir.add(testPsiFile)
                val file = testDir.findFile(testPsiFile.name)!!
                CodeInsightUtil.positionCursor(model.project, file, file)
            }
        }
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
            val result = Messages.showOkCancelDialog(
                project,
                message,
                "Requirements Error",
                "Install",
                "Cancel",
                null
            )
            if (result == Messages.CANCEL)
                return@invokeLater

            ProgressManager.getInstance().run(object : Backgroundable(project, "Installing requirements") {
                override fun run(indicator: ProgressIndicator) {
                    val installResult = installRequirements(pythonPath)

                    if (installResult.exitValue != 0) {
                        showErrorDialogLater(
                            project,
                            "Requirements installing failed.<br>" +
                                    "${installResult.stderr}<br><br>" +
                                    "Try to install with pip:<br>" +
                                    " ${requirements.joinToString("<br>")}",
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

fun getDirectoriesForSysPath(
    srcModule: Module,
    file: PyFile
): Pair<Set<String>, String> {
    val sources = ModuleRootManager.getInstance(srcModule).getSourceRoots(false).toMutableList()
    val ancestor = ProjectFileIndex.getInstance(file.project).getContentRootForFile(file.virtualFile)
    if (ancestor != null && !sources.contains(ancestor))
        sources.add(ancestor)

    // Collect sys.path directories with imported modules
    file.importTargets.forEach { importTarget ->
        importTarget.multiResolve().forEach {
            val element = it.element
            if (element != null) {
                val directory = element.parent
                if (directory is PsiDirectory) {
                    if (sources.any { source ->
                            val sourcePath = source.canonicalPath
                            if (source.isDirectory && sourcePath != null) {
                                directory.virtualFile.canonicalPath?.startsWith(sourcePath) ?: false
                            } else {
                                false
                            }
                        }) {
                        sources.add(directory.virtualFile)
                    }
                }
            }
        }
    }

    var importPath = ancestor?.let { VfsUtil.getParentDir(VfsUtilCore.getRelativeLocation(file.virtualFile, it)) } ?: ""
    if (importPath != "")
        importPath += "."

    return Pair(
        sources.map { it.path.replace("\\", "\\\\") }.toSet(),
        "${importPath}${file.name}".removeSuffix(".py").toPath().joinToString(".").replace("/", File.separator)
    )
}