package org.utbot.intellij.plugin.language.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.intellij.plugin.models.BaseTestsModel

class PythonTestsModel(
    project: Project,
    srcModule: Module,
    potentialTestModules: List<Module>,
    val functionsToDisplay: Set<PyFunction>,
    val containingClass: PyClass?,
    val focusedMethod: Set<PyFunction>?,
    val file: PyFile,
    val directoriesForSysPath: Set<String>,
    val currentPythonModule: String,
    var timeout: Long,
    var timeoutForRun: Long,
    var visitOnlySpecifiedSource: Boolean,
    val cgLanguageAssistant: CgLanguageAssistant,
    val pythonPath: String,
) : BaseTestsModel(
    project,
    srcModule,
    potentialTestModules
) {
    lateinit var testSourceRootPath: String
    lateinit var testFramework: TestFramework
    lateinit var selectedFunctions: Set<PyFunction>
}
