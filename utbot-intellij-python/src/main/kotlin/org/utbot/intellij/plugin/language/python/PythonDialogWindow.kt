package org.utbot.intellij.plugin.language.python

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.Panel
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.python.psi.*
import com.jetbrains.python.refactoring.classes.PyMemberInfoStorage
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import com.jetbrains.python.refactoring.classes.ui.PyMemberSelectionTable
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.intellij.plugin.settings.Settings
import java.awt.BorderLayout
import java.util.concurrent.TimeUnit
import org.utbot.intellij.plugin.ui.components.TestSourceDirectoryChooser
import org.utbot.intellij.plugin.ui.utils.createTestFrameworksRenderer
import java.awt.event.ActionEvent
import javax.swing.*


private const val WILL_BE_INSTALLED_LABEL = " (will be installed)"
private const val MINIMUM_TIMEOUT_VALUE_IN_SECONDS = 1
private const val ACTION_GENERATE = "Generate Tests"

class PythonDialogWindow(val model: PythonTestsModel) : DialogWrapper(model.project) {

    private val functionsTable = PyMemberSelectionTable(emptyList(), null, false)
    private val testSourceFolderField = TestSourceDirectoryChooser(model, model.file.virtualFile)
    private val timeoutSpinnerForTotalTimeout =
        JBIntSpinner(
            TimeUnit.MILLISECONDS.toSeconds(UtSettings.utBotGenerationTimeoutInMillis).toInt(),
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS,
            Int.MAX_VALUE,
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS
        )
    private val testFrameworks =
        ComboBox(DefaultComboBoxModel(model.cgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks.toTypedArray()))

    private lateinit var panel: DialogPanel

    init {
        title = "Generate Tests with UnitTestBot"
        isResizable = false

        model.cgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks.forEach {
            it.isInstalled = it.isInstalled || checkModuleIsInstalled(model.pythonPath, it.mainPackage)
        }

        init()
    }

    override fun createCenterPanel(): JComponent {

        panel = panel {
            row("Test sources root:") {
                component(testSourceFolderField)
            }
            row("Test framework:") {
                makePanelWithHelpTooltip(
                    testFrameworks,
                    null
                )
            }
            row("Timeout for all selected functions:") {
                cell {
                    component(timeoutSpinnerForTotalTimeout)
                    label("seconds")
                    component(ContextHelpLabel.create("Set the timeout for all test generation processes."))
                }
            }
            row("Generate test methods for:") {}
            row {
                scrollPane(functionsTable)
            }
        }

        updateFunctionsTable()
        updateTestFrameworksList()
        return panel
    }

    private fun updateTestFrameworksList() {
        testFrameworks.renderer = createTestFrameworksRenderer(WILL_BE_INSTALLED_LABEL)
    }

    private fun globalPyFunctionsToPyMemberInfo(
        project: Project,
        functions: Collection<PyFunction>
    ): List<PyMemberInfo<PyElement>> {
        val generator = PyElementGenerator.getInstance(project)
        val fakeClassName = generateRandomString(15)
        val newClass = generator.createFromText(
            LanguageLevel.getDefault(),
            PyClass::class.java,
            "class __FakeWrapperUtBotClass_$fakeClassName:\npass"
        )
        functions.forEach {
            newClass.add(it)
        }
        val storage = PyMemberInfoStorage(newClass)
        return storage.getClassMemberInfos(newClass)
    }

    private fun pyFunctionsToPyMemberInfo(
        project: Project,
        functions: Collection<PyFunction>,
        containingClass: PyClass?
    ): List<PyMemberInfo<PyElement>> {
        if (containingClass == null) {
            return globalPyFunctionsToPyMemberInfo(project, functions)
        }
        return PyMemberInfoStorage(containingClass).getClassMemberInfos(containingClass)
            .filter { it.member is PyFunction && fineFunction(it.member as PyFunction) }
    }

    private fun updateFunctionsTable() {
        val items = pyFunctionsToPyMemberInfo(model.project, model.functionsToDisplay, model.containingClass)
        updateMethodsTable(items)
        val height = functionsTable.rowHeight * (items.size.coerceAtMost(12) + 1)
        functionsTable.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

    private fun updateMethodsTable(allMethods: Collection<PyMemberInfo<PyElement>>) {
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

    private fun checkMembers(members: Collection<PyMemberInfo<PyElement>>) = members.forEach { it.isChecked = true }

    private fun Row.makePanelWithHelpTooltip(
        mainComponent: JComponent,
        contextHelpLabel: ContextHelpLabel?
    ): CellBuilder<JPanel> =
        component(Panel().apply {
            add(mainComponent, BorderLayout.LINE_START)
            contextHelpLabel?.let { add(it, BorderLayout.LINE_END) }
        })

    class OKOptionAction(private val okAction: Action) : AbstractAction(ACTION_GENERATE) {
        init {
            putValue(DEFAULT_ACTION, java.lang.Boolean.TRUE)
            putValue(FOCUSED_ACTION, java.lang.Boolean.TRUE)
        }
        override fun actionPerformed(e: ActionEvent?) {
            okAction.actionPerformed(e)
        }
    }

    private val okOptionAction: OKOptionAction get() = OKOptionAction(super.getOKAction())
    override fun getOKAction() = okOptionAction

    override fun doOKAction() {
        val selectedMembers = functionsTable.selectedMemberInfos
        model.selectedFunctions = selectedMembers.mapNotNull { it.member as? PyFunction }.toSet()
        model.testFramework = testFrameworks.item
        model.timeout = TimeUnit.SECONDS.toMillis(timeoutSpinnerForTotalTimeout.number.toLong())
        model.testSourceRootPath = testSourceFolderField.text
        model.projectType = ProjectType.Python

        val settings = model.project.service<Settings>()
        with(settings) {
            model.timeoutForRun = hangingTestsTimeout.timeoutMs
            model.runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour
        }

        super.doOKAction()
    }

    override fun doValidate(): ValidationInfo? {
        return testSourceFolderField.validatePath()
    }
}
