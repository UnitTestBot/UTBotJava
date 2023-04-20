package org.utbot.intellij.plugin.language.python

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.intellij.plugin.models.BaseTestsModel
import java.io.File

class PythonTestsModel(
    project: Project,
    srcModule: Module,
    potentialTestModules: List<Module>,
//    val functionsToDisplay: Set<PyFunction>,
    val elementsToDisplay: Set<PyElement>,
//    val containingClass: PyClass?,
//    val focusedMethod: Set<PyFunction>?,
    val focusedElements: Set<PyElement>?,
//    val file: PyFile,
//    val directoriesForSysPath: Set<String>,
//    val currentPythonModule: String,
    var timeout: Long,
    var timeoutForRun: Long,
    val cgLanguageAssistant: CgLanguageAssistant,
    val pythonPath: String,
    val names: Map<Pair<String, String>, PyElement>,
) : BaseTestsModel(
    project,
    srcModule,
    potentialTestModules
) {
    lateinit var testSourceRootPath: String
    lateinit var testFramework: TestFramework
//    lateinit var selectedFunctions: Set<PyFunction>
    var selectedElements: Set<PyElement> = emptySet()
    lateinit var runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour
}

data class PythonTestLocalModel(
    val project: Project,
    val timeout: Long,
    val timeoutForRun: Long,
    val cgLanguageAssistant: CgLanguageAssistant,
    val pythonPath: String,
    val testSourceRootPath: String,
    val testFramework: TestFramework,
    val selectedElements: Set<PyElement>,
    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour,
    val directoriesForSysPath: Set<String>,
    val currentPythonModule: String,
    val file: PyFile,
    val containingClass: PyClass?,
)