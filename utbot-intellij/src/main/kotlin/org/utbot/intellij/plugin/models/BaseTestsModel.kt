package org.utbot.intellij.plugin.models

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.utbot.framework.plugin.api.CodegenLanguage

open class BaseTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
) {
    open var testSourceRoot: VirtualFile? = null
    open var testPackageName: String? = null
    open lateinit var codegenLanguage: CodegenLanguage
}
