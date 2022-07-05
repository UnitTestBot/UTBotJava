package org.utbot.intellij.plugin.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.refactoring.util.classMembers.MemberInfo

data class PythonTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
    var selectedMethods: Set<MemberInfo>?, // to change
) {
    // var testSourceRoot: VirtualFile? = null
    // var testPackageName: String? = null
    // lateinit var testFramework: TestFramework
}
