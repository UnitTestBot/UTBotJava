package org.utbot.intellij.plugin.language.ts

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.utbot.framework.codegen.TestFramework
import org.utbot.intellij.plugin.models.BaseTestsModel
import org.utbot.language.ts.service.TsCoverageMode
import org.utbot.language.ts.settings.TsTestGenerationSettings.defaultTimeout

class TsTestsModel(
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
    lateinit var tsNycModulePath: String
    lateinit var tsModulePath: String
    var pathToNode: String = "node"
    var pathToNYC: String = "nyc"
    var pathToNPM: String = "npm"
    var coverageMode: TsCoverageMode = TsCoverageMode.FAST
}
