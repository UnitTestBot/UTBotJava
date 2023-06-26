package org.utbot.intellij.plugin.language.python

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
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
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.resolve.QualifiedNameFinder
import mu.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.jetbrains.kotlin.j2k.getContainingClass
import org.utbot.common.PathUtil.toPath
import org.utbot.common.appendHtmlLine
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.WarningTestsReportNotifier
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.python.PythonMethodHeader
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.newtyping.mypy.dropInitFile
import org.utbot.python.utils.RequirementsUtils.installRequirements
import org.utbot.python.utils.RequirementsUtils.requirements
import org.utbot.python.utils.camelToSnakeCase
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

const val DEFAULT_TIMEOUT_FOR_RUN_IN_MILLIS = 2000L

object PythonDialogProcessor {
    private val logger = KotlinLogging.logger {}

    enum class ProgressRange(val from : Double, val to: Double) {
        ANALYZE(from = 0.0, to = 0.1),
        SOLVING(from = 0.1, to = 0.95),
        CODEGEN(from = 0.95, to = 1.0),
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
        elementsToShow: Set<PyElement>,
        focusedElement: PyElement?,
        editor: Editor? = null,
    ) {
        editor?.let{
            runWriteAction {
                with(FileDocumentManager.getInstance()) {
                    saveDocument(it.document)
                }
            }
        }
        val pythonPath = getPythonPath(elementsToShow)
        if (pythonPath == null) {
            showErrorDialogLater(
                project,
                message = "Couldn't find Python interpreter",
                title = "Python test generation error"
            )
        } else {
            val dialog = createDialog(
                project,
                elementsToShow,
                focusedElement,
                pythonPath,
            )
            if (!dialog.showAndGet()) {
                return
            }
            createTests(project, dialog.model)
        }
    }

    private fun getPythonPath(elementsToShow: Set<PyElement>): String? {
        return findSrcModules(elementsToShow).first().sdk?.homePath
    }

    private fun createDialog(
        project: Project,
        elementsToShow: Set<PyElement>,
        focusedElement: PyElement?,
        pythonPath: String,
    ): PythonDialogWindow {
        val srcModules = findSrcModules(elementsToShow)
        val testModules = srcModules.flatMap {it.testModules(project)}
        val focusedElements = focusedElement
            ?.let { setOf(focusedElement.toUtPyTableItem()).filterNotNull() }
            ?.toSet()

        return PythonDialogWindow(
            PythonTestsModel(
                project,
                srcModules.first(),
                testModules,
                elementsToShow,
                focusedElements,
                project.service<Settings>().generationTimeoutInMillis,
                DEFAULT_TIMEOUT_FOR_RUN_IN_MILLIS,
                cgLanguageAssistant = PythonCgLanguageAssistant,
                pythonPath = pythonPath,
                names = elementsToShow.associateBy { Pair(it.fileName()!!, it.name!!) },
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestLocalModel): List<PythonMethodHeader> {
        return ReadAction.nonBlocking<List<PythonMethodHeader>> {
                model.selectedElements
                    .filter { model.selectedElements.contains(it) }
                    .flatMap {
                        when (it) {
                            is PyFunction -> listOf(it)
                            is PyClass -> it.methods.toList()
                            else -> emptyList()
                        }
                    }
                    .filter { fineFunction(it) }
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
                    .toList()
        }.executeSynchronously() ?: emptyList()
    }

    private fun groupPyElementsByModule(model: PythonTestsModel): Set<PythonTestLocalModel> {
        return runBlocking {
            readAction {
                model.selectedElements
                    .groupBy { it.containingFile }
                    .flatMap { fileGroup ->
                        fileGroup.value
                            .groupBy { it is PyClass }.values
                    }
                    .filter { it.isNotEmpty() }
                    .map {
                        val realElements = it.map { member -> model.names[Pair(member.fileName(), member.name)]!! }
                        val file = realElements.first().containingFile as PyFile
                        val srcModule = getSrcModule(realElements.first())

                        val (directoriesForSysPath, moduleToImport) = getDirectoriesForSysPath(srcModule, file)
                        PythonTestLocalModel(
                            model.project,
                            model.timeout,
                            model.timeoutForRun,
                            model.cgLanguageAssistant,
                            model.pythonPath,
                            model.testSourceRootPath,
                            model.testFramework,
                            realElements.toSet(),
                            model.runtimeExceptionTestsBehaviour,
                            directoriesForSysPath,
                            moduleToImport.dropInitFile(),
                            file,
                            realElements.first().getContainingClass() as PyClass?
                        )
                    }
                    .toSet()
            }
        }
    }

    private fun getOutputFileName(model: PythonTestLocalModel): String {
        val moduleName = model.currentPythonModule.camelToSnakeCase().replace('.', '_')
        return if (model.containingClass == null) {
            "test_$moduleName.py"
        } else {
            val className = model.containingClass.name?.camelToSnakeCase()?.replace('.', '_')
            "test_${moduleName}_$className.py"
        }
    }

    private fun getFileContent(file: PyFile): String {
        return runBlocking {
            readAction {
                getContentFromPyFile(file)
            }
        }
    }

    private fun createTests(project: Project, baseModel: PythonTestsModel) {
        ProgressManager.getInstance().run(object : Backgroundable(project, "Generate python tests") {
            override fun run(indicator: ProgressIndicator) {
                if (!LockFile.lock()) {
                    return
                }
                try {
                    indicator.isIndeterminate = false

                    groupPyElementsByModule(baseModel).forEach { model ->
                        updateIndicator(indicator, ProgressRange.ANALYZE, "Analyze code: read files")
                        val methods = findSelectedPythonMethods(model)
                        val content = getFileContent(model.file)

                        val requirementsList = requirements.toMutableList()
                        if (!model.testFramework.isInstalled) {
                            requirementsList += model.testFramework.mainPackage
                        }

                        val installer = IntellijRequirementsInstaller(project)

                        updateIndicator(indicator, ProgressRange.ANALYZE, "Analyze file ${model.file.name}")

                        updateIndicator(indicator, ProgressRange.SOLVING, "Generate test cases for file ${model.file.name}")

                        updateIndicator(indicator, ProgressRange.CODEGEN, "Generate code of tests for ${model.file.name}")

//                        val processor =

                        processTestGeneration(
                            pythonPath = model.pythonPath,
                            pythonFilePath = model.file.virtualFile.path,
                            pythonFileContent = content,
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
                                installer.installRequirements(model.pythonPath, requirementsList)
//                                askAndInstallRequirementsLater(model.project, model.pythonPath, requirementsList)
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
                    }
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

    private fun createPsiDirectoryForTestSourceRoot(model: PythonTestLocalModel): PsiDirectory {
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

    private fun writeGeneratedCodeToPsiDocument(generatedCode: String, model: PythonTestLocalModel) {
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

fun findSrcModules(elements: Collection<PyElement>): List<Module> {
    return elements.mapNotNull { it.module }.distinct()
}

fun getSrcModule(element: PyElement): Module {
    return ModuleUtilCore.findModuleForPsiElement(element) ?: error("Module for source class or function not found")
}

fun getFullName(element: PyElement): String {
    return QualifiedNameFinder.getQualifiedName(element) ?: error("Name for source class or function not found")
}

fun getContentFromPyFile(file: PyFile) = file.viewProvider.contents.toString()

/*
 * Returns set of sys paths and tested file import path
 */
fun getDirectoriesForSysPath(
    srcModule: Module,
    file: PyFile
): Pair<Set<String>, String> {
    return runBlocking {
        readAction {
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
                    val isRelativeImport =
                        importTarget.relativeLevel > 0  // If we have `from . import a` we don't need to add syspath
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
                val hasSitePackages =
                    (0 until (path.nameCount)).any { i -> path.subpath(i, i + 1).toString() == "site-packages" }
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

            Pair(
                sources.map { it.path }.toSet(),
                importStringPath
            )
        }
    }
}