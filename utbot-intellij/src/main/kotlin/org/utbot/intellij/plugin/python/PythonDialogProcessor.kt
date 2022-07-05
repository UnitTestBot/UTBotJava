package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.jetbrains.python.psi.PyFunction
import org.jetbrains.kotlin.idea.util.module
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow
import org.utbot.intellij.plugin.ui.utils.testModule

object PythonDialogProcessor {
    fun createDialogAndGenerateTests(
        project: Project,
        fileMethods: Set<PyFunction>,
        focusedMethod: PyFunction?,
    ) {
        val dialog = PythonDialogProcessor.createDialog(project, fileMethods, focusedMethod)
        if (!dialog.showAndGet()) {
            return
        }

        PythonDialogProcessor.createTests(project, dialog.model)
    }

    private fun createDialog(
        project: Project,
        fileMethods: Set<PyFunction>,
        focusedMethod: PyFunction?,
    ): PythonDialogWindow {
        val srcModule = findSrcModule(fileMethods)
        val testModule = srcModule.testModule(project)

        return PythonDialogWindow(
            PythonTestsModel(
                project,
                srcModule,
                testModule,
                fileMethods,
                if (focusedMethod != null) setOf(focusedMethod) else null,
            )
        )
    }

    private fun createTests(project: Project, model: PythonTestsModel) {
        // TODO
    }
}

fun findSrcModule(fileMethods: Set<PyFunction>): Module {
    val srcModules = fileMethods.mapNotNull { it.module }.distinct()
    return when (srcModules.size) {
        0 -> error("Module for source classes not found")
        1 -> srcModules.first()
        else -> error("Can not generate tests for classes from different modules")
    }
}
