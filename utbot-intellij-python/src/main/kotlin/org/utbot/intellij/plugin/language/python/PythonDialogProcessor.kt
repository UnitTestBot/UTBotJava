package org.utbot.intellij.plugin.language.python

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import kotlinx.coroutines.runBlocking
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
import org.utbot.python.PythonMethodHeader
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.utils.RequirementsUtils.installRequirements
import org.utbot.python.utils.RequirementsUtils.requirements
import org.utbot.python.utils.camelToSnakeCase
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
        file: PyFile,
        editor: Editor? = null,
    ) {
        editor?.let{
            runWriteAction {
                with(FileDocumentManager.getInstance()) {
                    saveDocument(it.document)
                }
            }
        }
        val pythonPath = getPythonPath(functionsToShow)
        if (pythonPath == null) {
            showErrorDialogLater(
                project,
                message = "Couldn't find Python interpreter",
                title = "Python test generation error"
            )
        } else {
            val dialog = createDialog(
                project,
                functionsToShow,
                containingClass,
                focusedMethod,
                file,
                pythonPath,
            )
            if (!dialog.showAndGet()) {
                return
            }
            createTests(project, dialog.model)
        }
    }

    private fun getPythonPath(functionsToShow: Set<PyFunction>): String? {
        return findSrcModule(functionsToShow).sdk?.homePath
    }

    private fun createDialog(
        project: Project,
        functionsToShow: Set<PyFunction>,
        containingClass: PyClass?,
        focusedMethod: PyFunction?,
        file: PyFile,
        pythonPath: String,
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
                cgLanguageAssistant = PythonCgLanguageAssistant,
                pythonPath = pythonPath,
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestsModel): List<PythonMethodHeader> {
        return runBlocking {
            readAction {
                val allFunctions: List<PyFunction> =
                    if (model.containingClass == null) {
                        model.file.topLevelFunctions
                    } else {
                        val classes = model.file.topLevelClasses
                        val myClass = classes.find { it.name == model.containingClass.name }
                            ?: error("Didn't find containing class")
                        myClass.methods.filterNotNull()
                    }
                val shownFunctions: Set<PythonMethodHeader> = allFunctions
                    .mapNotNull {
                        val functionName = it.name ?: return@mapNotNull null
                        val moduleFilename = it.containingFile.virtualFile?.canonicalPath ?: ""
                        val containingClassId = it.containingClass?.name?.let{ cls -> PythonClassId(cls) }
                        return@mapNotNull PythonMethodHeader(
                                functionName,
                                moduleFilename,
                                containingClassId,
                            )
                    }
                    .toSet()

                model.selectedFunctions.map { pyFunction ->
                    shownFunctions.find { pythonMethod ->
                        pythonMethod.name == pyFunction.name
                    } ?: error("Didn't find PythonMethod ${pyFunction.name}")
                }
            }
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
                    val methods = findSelectedPythonMethods(model)
                    val requirementsList = requirements.toMutableList()
                    if (!model.testFramework.isInstalled) {
                        requirementsList += model.testFramework.mainPackage
                    }

                    processTestGeneration(
                        pythonPath = model.pythonPath,
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
                        installingRequirementsAction = { indicator.text = "Installing requirements..." },
                        requirementsAreNotInstalledAction = {
                            askAndInstallRequirementsLater(model.project, model.pythonPath, requirementsList)
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
                        runtimeExceptionTestsBehaviour = model.runtimeExceptionTestsBehaviour,
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

    private fun askAndInstallRequirementsLater(project: Project, pythonPath: String, requirementsList: List<String>) {
        val message = """
            Some requirements are not installed.
            Requirements: <br>
            ${requirementsList.joinToString("<br>")}
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
                    val installResult = installRequirements(pythonPath, requirementsList)

                    if (installResult.exitValue != 0) {
                        showErrorDialogLater(
                            project,
                            "Requirements installing failed.<br>" +
                                    "${installResult.stderr}<br><br>" +
                                    "Try to install with pip:<br>" +
                                    " ${requirementsList.joinToString("<br>")}",
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

/*
 * Returns set of sys paths and tested file import path
 */
fun getDirectoriesForSysPath(
    srcModule: Module,
    file: PyFile
): Pair<Set<String>, String> {
    val sources = ModuleRootManager.getInstance(srcModule).getSourceRoots(false).toMutableList()
    val ancestor = ProjectFileIndex.getInstance(file.project).getContentRootForFile(file.virtualFile)
    if (ancestor != null)
        sources.add(ancestor)

    // Collect sys.path directories with imported modules
    val importedPaths = emptyList<VirtualFile>().toMutableList()

    // 1. import <module>
    file.importTargets.forEach { importTarget ->
        importTarget.multiResolve().forEach {
            val element = it.element
            if (element != null) {
                val directory = element.parent
                if (directory is PsiDirectory) {
                    // If we have `import a.b.c` we need to add syspath to module `a` only
                    val additionalLevel = importTarget.importedQName?.componentCount?.dec() ?: 0
                    directory.topParent(additionalLevel)?.let { dir ->
                        importedPaths.add(dir.virtualFile)
                    }
                }
            }
        }
    }

    // 2. from <module> import ...
    file.fromImports.forEach { importTarget ->
        importTarget.resolveImportSourceCandidates().forEach {
            val directory = it.parent
            val isRelativeImport = importTarget.relativeLevel > 0  // If we have `from . import a` we don't need to add syspath
            if (directory is PsiDirectory && !isRelativeImport) {
                // If we have `from a.b.c import d` we need to add syspath to module `a` only
                val additionalLevel = importTarget.importSourceQName?.componentCount?.dec() ?: 0
                directory.topParent(additionalLevel)?.let { dir ->
                    importedPaths.add(dir.virtualFile)
                }
            }
        }
    }

    // Select modules only from this project but not from installation directory
    importedPaths.forEach {
        val path = it.toNioPath()
        val hasSitePackages = (0 until(path.nameCount)).any { i -> path.subpath(i, i+1).toString() == "site-packages"}
        if (it.isProjectSubmodule(ancestor) && !hasSitePackages) {
            sources.add(it)
        }
    }

    val fileName = file.name.removeSuffix(".py")
    val importPath = ancestor?.let {
        VfsUtil.getParentDir(
            VfsUtilCore.getRelativeLocation(file.virtualFile, it)
        )
    } ?: ""
    val importStringPath = listOf(
        importPath.toPath().joinToString("."),
        fileName
    )
        .filterNot { it.isEmpty() }
        .joinToString(".")

    return Pair(
        sources.map { it.path }.toSet(),
        importStringPath
    )
}