package org.utbot.intellij.plugin.language.js

import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.core.getPackage
import org.utbot.framework.codegen.TestFramework
import org.utbot.intellij.plugin.models.BaseTestsModel
import settings.JsTestGenerationSettings.defaultTimeout


val PsiClass.packageName: String get() = this.containingFile.containingDirectory.getPackage()?.qualifiedName ?: ""

class JsTestsModel(
    project: Project,
    srcModule: Module,
    potentialTestModules: List<Module>,
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
}