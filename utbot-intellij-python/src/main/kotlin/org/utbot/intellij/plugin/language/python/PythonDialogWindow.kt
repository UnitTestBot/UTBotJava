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
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.intellij.plugin.language.python.table.UtPyClassItem
import org.utbot.intellij.plugin.language.python.table.UtPyFunctionItem
import org.utbot.intellij.plugin.language.python.table.UtPyMemberSelectionTable
import org.utbot.intellij.plugin.language.python.table.UtPyTableItem
import org.utbot.intellij.plugin.settings.Settings
import java.awt.BorderLayout
import java.util.concurrent.TimeUnit
import org.utbot.intellij.plugin.ui.components.TestSourceDirectoryChooser
import org.utbot.intellij.plugin.ui.utils.createTestFrameworksRenderer
import java.awt.event.ActionEvent
import javax.swing.*


private const val WILL_BE_INSTALLED_LABEL = " (will be installed)"
private const val MINIMUM_TIMEOUT_VALUE_IN_SECONDS = 1
private const val STEP_TIMEOUT_VALUE_IN_SECONDS = 5
private const val ACTION_GENERATE = "Generate Tests"

class PythonDialogWindow(val model: PythonTestsModel) : DialogWrapper(model.project) {

    private val pyElementsTable = UtPyMemberSelectionTable(emptyList())
    private val testSourceFolderField = TestSourceDirectoryChooser(model)
    private val timeoutSpinnerForTotalTimeout =
        JBIntSpinner(
            TimeUnit.MILLISECONDS.toSeconds(UtSettings.utBotGenerationTimeoutInMillis).toInt(),
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS,
            Int.MAX_VALUE,
            STEP_TIMEOUT_VALUE_IN_SECONDS
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
                scrollPane(pyElementsTable)
            }
        }

        updatePyElementsTable()
        updateTestFrameworksList()
        return panel
    }

    private fun updateTestFrameworksList() {
        testFrameworks.renderer = createTestFrameworksRenderer(WILL_BE_INSTALLED_LABEL)
    }

    private fun updatePyElementsTable() {
        val functions = model.elementsToDisplay.filterIsInstance<PyFunction>()
        val classes = model.elementsToDisplay.filterIsInstance<PyClass>()
        val functionItems = functions
            .groupBy { it.containingClass }
            .flatMap { (_, pyFuncs) ->
                pyFuncs.map {UtPyFunctionItem(it)}
            }
        val classItems = classes.map {
            UtPyClassItem(it)
        }
        val items = classItems + functionItems
        updateMethodsTable(items)
        val height = pyElementsTable.rowHeight * (items.size.coerceAtMost(12) + 1)
        pyElementsTable.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

    private fun updateMethodsTable(allMethods: Collection<UtPyTableItem>) {
        val focusedNames = model.focusedElements?.map { it.idName }
        val selectedMethods = allMethods.filter {
            focusedNames?.contains(it.idName) ?: false
        }

        if (selectedMethods.isEmpty()) {
            checkMembers(allMethods)
        } else {
            checkMembers(selectedMethods)
        }

        pyElementsTable.setItems(allMethods)
    }

    private fun checkMembers(members: Collection<UtPyTableItem>) = members.forEach { it.isChecked = true }

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
        val selectedMembers = pyElementsTable.selectedMemberInfos
        model.selectedElements = selectedMembers.mapNotNull { it.content }.toSet()
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
