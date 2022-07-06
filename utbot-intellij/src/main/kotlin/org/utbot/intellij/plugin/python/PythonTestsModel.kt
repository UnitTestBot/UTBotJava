package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import org.utbot.framework.codegen.TestFramework
import org.utbot.intellij.plugin.ui.utils.BaseTestsModel

class PythonTestsModel(
    project: Project,
    srcModule: Module,
    testModule: Module,
    val fileMethods: Set<PyMemberInfo<PyElement>>?,
    val focusedMethod: Set<PyFunction>?,
): BaseTestsModel(
    project,
    srcModule,
    testModule
) {
     lateinit var testFramework: TestFramework
}
