package org.utbot.intellij.plugin.models

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.plugin.api.CodegenLanguage


open class BaseTestsModel(
    val project: Project,
) {
    var testSourceRoot: VirtualFile? = null
    var testPackageName: String? = null
    open var sourceRootHistory : MutableList<String> = mutableListOf()
    open lateinit var codegenLanguage: CodegenLanguage
    open lateinit var projectType: ProjectType
}
