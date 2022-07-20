package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import org.utbot.framework.codegen.TestFramework
import org.utbot.intellij.plugin.ui.utils.BaseTestsModel

class PythonTestsModel(
    project: Project,
    srcModule: Module,
    testModule: Module,
    val functionsToDisplay: Set<PyFunction>,
    val containingClass: PyClass?,
    val focusedMethod: Set<PyFunction>?,
    val file: PyFile,
    var moduleToImport: String
): BaseTestsModel(
    project,
    srcModule,
    testModule
) {
     lateinit var testFramework: TestFramework
     lateinit var selectedFunctions: Set<PyFunction>
     lateinit var directoriesForSysPath: List<String>
}
