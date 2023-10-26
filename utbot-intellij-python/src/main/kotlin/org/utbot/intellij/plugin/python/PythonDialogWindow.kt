package org.utbot.intellij.plugin.python

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.intellij.plugin.python.settings.PythonTestFrameworkMapper
import org.utbot.intellij.plugin.python.settings.loadStateFromModel
import org.utbot.intellij.plugin.python.table.UtPyClassItem
import org.utbot.intellij.plugin.python.table.UtPyFunctionItem
import org.utbot.intellij.plugin.python.table.UtPyMemberSelectionTable
import org.utbot.intellij.plugin.python.table.UtPyTableItem
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.components.TestSourceDirectoryChooser
import org.utbot.intellij.plugin.ui.utils.createTestFrameworksRenderer
import java.util.concurrent.TimeUnit
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

private const val WILL_BE_INSTALLED_LABEL = " (will be installed)"
private const val MINIMUM_TIMEOUT_VALUE_IN_SECONDS = 5
private const val STEP_TIMEOUT_VALUE_IN_SECONDS = 5
private const val ACTION_GENERATE = "Generate Tests"

class PythonDialogWindow(val model: PythonTestsModel) : DialogWrapper(model.project) {

    private val pyElementsTable = UtPyMemberSelectionTable(emptyList())
    private val testSourceFolderField = TestSourceDirectoryChooser(model)
    private val timeoutSpinnerForTotalTimeout =
        JBIntSpinner(
            TimeUnit.MILLISECONDS.toSeconds(model.timeout).toInt(),
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS,
            Int.MAX_VALUE,
            STEP_TIMEOUT_VALUE_IN_SECONDS
        )
    private val testFrameworks =
        ComboBox(DefaultComboBoxModel(model.cgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks.toTypedArray()))

    private lateinit var panel: DialogPanel
    private lateinit var currentFrameworkItem: TestFramework

    init {
        title = "Generate Tests with UnitTestBot"
        isResizable = false

        model.cgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks.forEach {
            it.isInstalled = it.isInstalled || checkModuleIsInstalled(model.pythonPath, it.mainPackage)
        }

        init()
        setOKButtonText(ACTION_GENERATE)
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test sources root:") {
                cell(testSourceFolderField).align(Align.FILL)
            }
            row("Testing framework:") {
                cell(testFrameworks)
            }
            row("Test generation timeout:") {
                cell(BorderLayoutPanel().apply {
                    addToLeft(timeoutSpinnerForTotalTimeout)
                    addToRight(JBLabel("seconds per group"))
                })
                contextHelp("Set the timeout for all test generation processes per class or top level functions in one module to complete.")
            }
            row("Generate tests for:") {}
            row {
                cell(JBScrollPane(pyElementsTable)).align(Align.FILL)
            }
        }

        initDefaultValues()
        updatePyElementsTable()
        return panel
    }

    private fun initDefaultValues() {
        val settings = model.project.service<Settings>()

        val installedTestFramework = PythonTestFrameworkMapper.allItems.singleOrNull { it.isInstalled }
        val testFramework = PythonTestFrameworkMapper.handleUnknown(settings.testFramework)
        currentFrameworkItem = installedTestFramework ?: testFramework

        updateTestFrameworksList()
    }

    private fun updateTestFrameworksList() {
        testFrameworks.item = currentFrameworkItem
        testFrameworks.renderer = createTestFrameworksRenderer(WILL_BE_INSTALLED_LABEL)
    }

    private fun updatePyElementsTable() {
        val functions = model.elementsToDisplay.filterIsInstance<PyFunction>()
        val classes = model.elementsToDisplay.filterIsInstance<PyClass>()
        val functionItems = functions
            .groupBy { it.containingClass }
            .flatMap { (_, pyFuncs) ->
                pyFuncs.map { UtPyFunctionItem(it) }
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

        loadStateFromModel(settings, model)

        super.doOKAction()
    }

    override fun doValidate(): ValidationInfo? {
        return testSourceFolderField.validatePath()
    }
}
