package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo

data class PythonTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
    val fileMethods: Set<PyMemberInfo<PyElement>>?,
    val focusedMethod: Set<PyFunction>?,
) {
     var testSourceRoot: VirtualFile? = null
    // var testPackageName: String? = null
    // lateinit var testFramework: TestFramework
}
