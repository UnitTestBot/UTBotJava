package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyClass
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.utbot.common.PathUtil.toPath
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModule
import org.utbot.python.PythonCodeCollector
import org.utbot.python.code.PythonCode
import org.utbot.python.code.PythonCode.Companion.getFromString
import org.utbot.python.code.PythonCodeGenerator.generateTestCode
import org.utbot.python.code.PythonCodeGenerator.saveToFile
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestCaseGenerator


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

        return PythonDialogWindow(
            PythonTestsModel(
                project,
                srcModule,
                testModule,
                functionsToShow,
                containingClass,
                if (focusedMethod != null) setOf(focusedMethod) else null,
                file,
                getDefaultModuleToImport(file)
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestsModel): List<PythonMethod> {
        val code = getPyCodeFromPyFile(model.file)

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

    private fun createTests(project: Project, model: PythonTestsModel) {
        ProgressManager.getInstance().run(object : Backgroundable(project, "Generate python tests") {
            override fun run(indicator: ProgressIndicator) {

                // PythonCodeCollector.refreshProjectClassesList(model.project.basePath!!)
                PythonCodeCollector.refreshProjectClassesList(model.file.virtualFile.path)

                val pythonMethods = findSelectedPythonMethods(model)
                val testSourceRoot = model.testSourceRoot!!.path

                val testCaseGenerator = PythonTestCaseGenerator.apply {
                    init(
                        testSourceRoot,
                        model.directoriesForSysPath,
                        model.moduleToImport,
                        model.srcModule.sdk?.homePath ?: error("Couldn't find Python interpreter")
                    )
                }

                val tests = pythonMethods.map { method ->
                    testCaseGenerator.generate(method)
                }
                val notEmptyTests = tests.filter { it.executions.isNotEmpty() || it.errors.isNotEmpty() }
                val functionsWithoutTests = tests.mapNotNull {
                    if (it.executions.isEmpty() && it.errors.isEmpty()) it.method.name else null
                }

                if (functionsWithoutTests.isNotEmpty()) {
                    showErrorDialogLater(
                        project,
                        message = "Cannot create tests for the following functions: " + functionsWithoutTests.joinToString { it },
                        title = "Python test generation error"
                    )
                }

                notEmptyTests.forEach {
                    val testCode = generateTestCode(it, model.directoriesForSysPath, model.moduleToImport)
                    saveToFile("$testSourceRoot/test_${it.method.name}.py", testCode)
                }
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

fun getDefaultModuleToImport(file: PyFile): String {
    val importPath = file.virtualFile?.let { absoluteFilePath ->
        ProjectFileIndex.SERVICE.getInstance(file.project).getContentRootForFile(absoluteFilePath)?.let {absoluteProjectPath ->
            VfsUtil.getParentDir(VfsUtilCore.getRelativeLocation(absoluteFilePath, absoluteProjectPath))
        }
    } ?: ""

    return "${importPath}.${file.name}".dropLast(3).toPath().joinToString(".")
}

fun getPyCodeFromPyFile(file: PyFile): PythonCode {
    val content = file.viewProvider.contents.toString()
    return getFromString(content)
}