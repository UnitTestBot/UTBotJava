package org.utbot.intellij.plugin.python

import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyFunction
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow

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
    ): GenerateTestsDialogWindow {

        return PythonDialogWindow(
            PythonTestsModel(
                // TODO
            )
        )
    }

    private fun createTests(project: Project, model: PythonTestsModel) {
        // TODO
    }
}