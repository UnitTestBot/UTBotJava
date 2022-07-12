package org.utbot.intellij.plugin.python

import org.utbot.python.PythonEvaluation
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import org.utbot.intellij.plugin.ui.utils.testModule

object PythonDialogProcessor {
    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<PyMemberInfo<PyElement>>,
        focusedMethod: PyFunction?,
        files: Set<PyFile>
    ) {
        val dialog = PythonDialogProcessor.createDialog(project, srcModule, fileMethods, focusedMethod, files)
        if (!dialog.showAndGet()) {
            return
        }

        PythonDialogProcessor.createTests(project, dialog.model)
    }

    private fun createDialog(
        project: Project,
        srcModule: Module,
        fileMethods: Set<PyMemberInfo<PyElement>>,
        focusedMethod: PyFunction?,
        files: Set<PyFile>
    ): PythonDialogWindow {
        val testModule = srcModule.testModule(project)

        return PythonDialogWindow(
            PythonTestsModel(
                project,
                srcModule,
                testModule,
                fileMethods,
                if (focusedMethod != null) setOf(focusedMethod) else null,
                files
            )
        )
    }

    private fun createTests(project: Project, model: PythonTestsModel) {
    }
}
