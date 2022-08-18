package org.utbot.intellij.plugin.models

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.utbot.framework.plugin.api.CodegenLanguage

open class BaseTestsModel(
    val project: Project,
    val srcModule: Module,
    val potentialTestModules: List<Module>,
) {
    // GenerateTestsModel is supposed to be created with non-empty list of potentialTestModules.
    // Otherwise, the error window is supposed to be shown earlier.
    var testModule: Module = potentialTestModules.firstOrNull() ?: error("Empty list of test modules in model")

    open var testSourceRoot: VirtualFile? = null
    open var testPackageName: String? = null
    open lateinit var codegenLanguage: CodegenLanguage

    fun setSourceRootAndFindTestModule(newTestSourceRoot: VirtualFile?) {
        requireNotNull(newTestSourceRoot)
        testSourceRoot = newTestSourceRoot
        testModule = ModuleUtil.findModuleForFile(newTestSourceRoot, project)
            ?: error("Could not find module for $newTestSourceRoot")
    }

}
