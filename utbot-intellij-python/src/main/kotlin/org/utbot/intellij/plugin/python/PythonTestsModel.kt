package org.utbot.intellij.plugin.python

import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.intellij.plugin.python.table.UtPyTableItem
import org.utbot.intellij.plugin.models.BaseTestsModel

class PythonTestsModel(
    project: Project,
    val elementsToDisplay: Set<PyElement>,
    val focusedElements: Set<UtPyTableItem>?,
    var timeout: Long,
    var timeoutForRun: Long,
    val cgLanguageAssistant: CgLanguageAssistant,
    val pythonPath: String,
    val names: Map<Pair<String, String>, PyElement>,
) : BaseTestsModel(
    project,
) {
    lateinit var testSourceRootPath: String
    lateinit var testFramework: TestFramework
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