package org.utbot.intellij.plugin.python

import com.intellij.lang.jvm.actions.updateMethodParametersRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.refactoring.classes.PyMemberInfoStorage
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import com.jetbrains.python.refactoring.classes.ui.PyMemberSelectionTable
import javax.swing.JComponent

class PythonDialogWindow(val model: PythonTestsModel): DialogWrapper(model.project) {

    private val functionsTable = PyMemberSelectionTable(emptyList(), null, false)

    private val testSourceFolderField = PythonTestFolderComboWithBrowseButton(model)

    private lateinit var panel: DialogPanel

    init {
        title = "Generate tests with UtBot"
        setResizable(false)
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                component(testSourceFolderField)
            }
            row("Generate test methods for:") {}
            row {
                scrollPane(functionsTable)
            }
        }

        initDefaultValues()
        updateFunctionsTable()
        return panel
    }

    private fun initDefaultValues() {
    }
    private fun setListeners() {
    }

    private fun updateFunctionsTable() {
        val items = pyFunctionsToPyMemberInfo(model.project, model.fileMethods!!)
        updateMethodsTable(items)
    }

    private fun updateMethodsTable(allMethods: List<PyMemberInfo<PyElement>>) {
        val focusedNames = model.focusedMethod?.map { it.name }
        val selectedMethods = allMethods.filter {
            focusedNames?.contains(it.member.name) ?: false
        }

        if (selectedMethods.isEmpty()) {
            checkMembers(allMethods)
        } else {
            checkMembers(selectedMethods)
        }

        functionsTable.setMemberInfos(allMethods)
    }

    private fun checkMembers(members: List<PyMemberInfo<PyElement>>) = members.forEach { it.isChecked = true }
}


fun pyFunctionsToPyMemberInfo(project: Project, functions: Set<PyFunction>): List<PyMemberInfo<PyElement>> {
    val generator = PyElementGenerator.getInstance(project)
    val newClass = generator.createFromText(
        LanguageLevel.getDefault(),
        PyClass::class.java,
        "class A:\npass"
    )
    functions.forEach {
        newClass.add(it)
    }
    val storage = PyMemberInfoStorage(newClass)
    val infos = storage.getClassMemberInfos(newClass)
    return infos
}