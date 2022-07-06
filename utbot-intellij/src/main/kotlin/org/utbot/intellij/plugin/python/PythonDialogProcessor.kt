package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import org.jetbrains.kotlin.idea.util.module
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow
import org.utbot.intellij.plugin.ui.utils.testModule

object PythonDialogProcessor {
    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<PyMemberInfo<PyElement>>,
        focusedMethod: PyFunction?,
    ) {
        val dialog = PythonDialogProcessor.createDialog(project, srcModule, fileMethods, focusedMethod)
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
    ): PythonDialogWindow {
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
