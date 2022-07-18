package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyClass
import org.jetbrains.kotlin.idea.util.module
import org.utbot.intellij.plugin.ui.utils.testModule
import org.utbot.python.PythonCode
import org.utbot.python.PythonCode.Companion.getFromString
import org.utbot.python.PythonCodeGenerator.generateTestCode
import org.utbot.python.PythonCodeGenerator.saveToFile
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
                setOf(file)
            )
        )
    }

    private fun findSelectedPythonMethods(model: PythonTestsModel): List<PythonMethod> {
        if (model.files.size != 1) {
            error("Can't process several files yet")
        }

        val code = getPyCodeFromPyFile(model.files.first())

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
                val pythonMethods = findSelectedPythonMethods(model)
                val testSourceRoot = model.testSourceRoot!!.path

                val tests = pythonMethods.map { method ->
                    PythonTestCaseGenerator.generate(method, testSourceRoot)
                }

                val x = tests.toList()

                tests.forEach {
                    val testCode = generateTestCode(it)
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

fun getPyCodeFromPyFile(file: PyFile): PythonCode {
    val content = file.viewProvider.contents.toString()
    return getFromString(content)
}