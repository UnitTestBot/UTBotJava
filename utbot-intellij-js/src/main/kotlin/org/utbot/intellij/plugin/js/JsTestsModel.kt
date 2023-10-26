package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.intellij.plugin.models.BaseTestsModel
import service.coverage.CoverageMode
import settings.JsTestGenerationSettings.defaultTimeout

class JsTestsModel(
    project: Project,
    val potentialTestModules: List<Module>,
    val file: JSFile,
    val fileMethods: Set<JSMemberInfo>,
    var selectedMethods: Set<JSMemberInfo>,
) : BaseTestsModel(
    project
) {
    var testModule: Module = potentialTestModules.firstOrNull() ?: error("Empty list of test modules in model")

    var timeout = defaultTimeout

    lateinit var testFramework: TestFramework
    lateinit var containingFilePath: String
    var pathToNode: String = "node"
    var pathToNYC: String = "nyc"
    var pathToNPM: String = "npm"
    var coverageMode: CoverageMode = CoverageMode.FAST

    fun setSourceRootAndFindTestModule(newTestSourceRoot: VirtualFile?) {
        requireNotNull(newTestSourceRoot)
        testSourceRoot = newTestSourceRoot
        var target = newTestSourceRoot
        while (target != null && target is FakeVirtualFile) {
            target = target.parent
        }
        if (target == null) {
            error("Could not find module for $newTestSourceRoot")
        }

        testModule = ModuleUtil.findModuleForFile(target, project)
            ?: error("Could not find module for $newTestSourceRoot")
    }
}
