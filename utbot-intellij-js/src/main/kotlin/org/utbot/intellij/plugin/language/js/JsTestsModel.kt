package org.utbot.intellij.plugin.language.js

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.intellij.plugin.models.BaseTestsModel
import service.coverage.CoverageMode
import settings.JsTestGenerationSettings.defaultTimeout

class JsTestsModel(
    project: Project,
    srcModule: Module,
    potentialTestModules: List<Module>,
    val file: JSFile,
    val fileMethods: Set<JSMemberInfo>,
    var selectedMethods: Set<JSMemberInfo>,
) : BaseTestsModel(
    project, srcModule, potentialTestModules, emptySet()
) {

    var timeout = defaultTimeout

    lateinit var testFramework: TestFramework
    lateinit var containingFilePath: String
    var pathToNode: String = "node"
    var pathToNYC: String = "nyc"
    var pathToNPM: String = "npm"
    var coverageMode: CoverageMode = CoverageMode.FAST
}
