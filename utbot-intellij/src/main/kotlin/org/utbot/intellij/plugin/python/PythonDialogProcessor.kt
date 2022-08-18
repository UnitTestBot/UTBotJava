package org.utbot.intellij.plugin.python

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyClass
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModule
import org.utbot.intellij.plugin.ui.WarningTestsReportNotifier
import org.utbot.python.code.PythonCode
import org.utbot.python.code.PythonCode.Companion.getFromString
import org.utbot.python.PythonMethod
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration

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
        val testModule = srcModule.testModule(project)
        val (directoriesForSysPath, moduleToImport) = getDirectoriesForSysPath(srcModule, file)

        return PythonDialogWindow(
            PythonTestsModel(
                project,
                srcModule,
                testModule,
                functionsToShow,
                containingClass,
                if (focusedMethod != null) setOf(focusedMethod) else null,
                file,
                directoriesForSysPath,
                moduleToImport,
                UtSettings.utBotGenerationTimeoutInMillis
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestsModel): List<PythonMethod> {
        val code = getPyCodeFromPyFile(model.file, model.currentPythonModule)

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
                processTestGeneration(
                    pythonPath = model.srcModule.sdk?.homePath ?: error("Couldn't find Python interpreter"),
                    testSourceRoot = model.testSourceRoot!!.path,
                    pythonFilePath = model.file.virtualFile.path,
                    pythonFileContent = getContentFromPyFile(model.file),
                    directoriesForSysPath = model.directoriesForSysPath,
                    currentPythonModule = model.currentPythonModule,
                    pythonMethods = findSelectedPythonMethods(model),
                    containingClassName = model.containingClass?.name,
                    timeout = model.timeout,
                    testFramework = model.testFramework,
                    codegenLanguage = model.codegenLanguage,
                    outputFilename = getOutputFileName(model),
                    isCanceled = { indicator.isCanceled },
                    checkingRequirementsAction = { indicator.text = "Checking requirements" },
                    requirementsAreNotInstalledAction = {
                        showErrorDialogLater(
                            project,
                            message = "Requirements are not installed",
                            title = "Python test generation error"
                        )
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
                    generatedFileWithTestsAction = {
                        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(it)
                        if (virtualFile != null) {
                            invokeAndWaitIfNeeded {
                                OpenFileDescriptor(model.project, virtualFile).navigate(true)
                            }
                        }
                    },
                    processMypyWarnings = { WarningTestsReportNotifier.notify(it) },
                    startedCleaningAction = { indicator.text = "Cleaning up..." }
                )
            }
        })
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

fun getPyCodeFromPyFile(file: PyFile, pythonModule: String): PythonCode {
    val content = getContentFromPyFile(file)
    return getFromString(content, pythonModule = pythonModule)
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
        "${importPath}${file.name}".removeSuffix(".py").toPath().joinToString(".")
    )
}