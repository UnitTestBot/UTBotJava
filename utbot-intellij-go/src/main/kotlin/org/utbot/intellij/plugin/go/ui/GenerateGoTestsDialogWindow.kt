package org.utbot.intellij.plugin.go.ui

import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoMethodDeclaration
import com.goide.refactor.ui.GoDeclarationInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.utbot.go.logic.GoUtTestsGenerationConfig
import org.utbot.go.logic.TestsGenerationMode
import org.utbot.intellij.plugin.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.settings.Settings
import java.text.ParseException
import java.util.concurrent.TimeUnit
import javax.swing.JCheckBox
import javax.swing.JComponent

private const val MINIMUM_ALL_EXECUTION_TIMEOUT_SECONDS = 1
private const val ALL_EXECUTION_TIMEOUT_SECONDS_SPINNER_STEP = 10

private const val MINIMUM_NUMBER_OF_FUZZING_PROCESSES = 1
private const val NUMBER_OF_FUZZING_PROCESSES_STEP = 1

// This class is highly inspired by GenerateTestsDialogWindow.
class GenerateGoTestsDialogWindow(val model: GenerateGoTestsModel) : DialogWrapper(model.project) {

    private val targetInfos = model.targetFunctions.toInfos()
    private val targetFunctionsTable = GoFunctionsSelectionTable(targetInfos).apply {
        val height = this.rowHeight * (targetInfos.size.coerceAtMost(12) + 1)
        this.preferredScrollableViewportSize = JBUI.size(-1, height)
    }
    private val numberOfFuzzingProcessesSpinner: JBIntSpinner = JBIntSpinner(
        GoUtTestsGenerationConfig.DEFAULT_NUMBER_OF_FUZZING_PROCESSES,
        MINIMUM_NUMBER_OF_FUZZING_PROCESSES,
        Int.MAX_VALUE,
        NUMBER_OF_FUZZING_PROCESSES_STEP
    )
    private val fuzzingMode = JCheckBox("Fuzzing mode")

    private val allFunctionExecutionTimeoutSecondsSpinner =
        JBIntSpinner(
            TimeUnit.MILLISECONDS.toSeconds(GoUtTestsGenerationConfig.DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS).toInt(),
            MINIMUM_ALL_EXECUTION_TIMEOUT_SECONDS,
            Int.MAX_VALUE,
            ALL_EXECUTION_TIMEOUT_SECONDS_SPINNER_STEP
        )

    private lateinit var panel: DialogPanel

    init {
        title = "Generate Tests with UnitTestBot"
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Timeout for all functions:") {
                cell(allFunctionExecutionTimeoutSecondsSpinner)
                cell(JBLabel("seconds"))
            }
            row("Number of fuzzing processes:") {
                cell(numberOfFuzzingProcessesSpinner)
            }
            row {
                cell(fuzzingMode)
                contextHelp("Stop test generation when a panic or error occurs (only one test will be generated for one of these cases)")
            }
            row("Generate test methods for:") {}
            row {
                scrollCell(targetFunctionsTable).align(Align.FILL)
            }
        }
        updateFunctionsOrMethodsTable()
        return panel
    }

    override fun doOKAction() {
        model.selectedFunctions =
            targetFunctionsTable.selectedMemberInfos.fromInfos().filterIsInstance<GoFunctionDeclaration>().toSet()
        model.selectedMethods =
            targetFunctionsTable.selectedMemberInfos.fromInfos().filterIsInstance<GoMethodDeclaration>().toSet()
        try {
            numberOfFuzzingProcessesSpinner.commitEdit()
            allFunctionExecutionTimeoutSecondsSpinner.commitEdit()
        } catch (_: ParseException) {
        }
        model.numberOfFuzzingProcess = numberOfFuzzingProcessesSpinner.number
        model.mode = if (fuzzingMode.isSelected) {
            TestsGenerationMode.FUZZING_MODE
        } else {
            TestsGenerationMode.DEFAULT
        }
        val settings = model.project.service<Settings>()
        with(settings) {
            model.eachFunctionExecutionTimeoutMillis = hangingTestsTimeout.timeoutMs
        }
        model.allFunctionExecutionTimeoutMillis =
            TimeUnit.SECONDS.toMillis(allFunctionExecutionTimeoutSecondsSpinner.number.toLong())
        super.doOKAction()
    }

    private fun updateFunctionsOrMethodsTable() {
        val focusedTargetFunctionsNames = model.focusedTargetFunctions.map { it.name }.toSet()
        val selectedInfos = targetInfos.filter {
            it.declaration.name in focusedTargetFunctionsNames
        }
        if (selectedInfos.isEmpty()) {
            checkInfos(targetInfos)
        } else {
            checkInfos(selectedInfos)
        }
        targetFunctionsTable.setMemberInfos(targetInfos)
    }

    private fun checkInfos(infos: Collection<GoDeclarationInfo>) {
        infos.forEach { it.isChecked = true }
    }

    private fun Collection<GoFunctionOrMethodDeclaration>.toInfos(): Set<GoDeclarationInfo> =
        this.map { GoDeclarationInfo(it) }.toSet()

    private fun Collection<GoDeclarationInfo>.fromInfos(): Set<GoFunctionOrMethodDeclaration> =
        this.map { it.declaration as GoFunctionOrMethodDeclaration }.toSet()

    @Suppress("DuplicatedCode") // This method is highly inspired by GenerateTestsDialogWindow.doValidate().
    override fun doValidate(): ValidationInfo? {
        targetFunctionsTable.tableHeader?.background = UIUtil.getTableBackground()
        targetFunctionsTable.background = UIUtil.getTableBackground()
        if (targetFunctionsTable.selectedMemberInfos.isEmpty()) {
            targetFunctionsTable.tableHeader?.background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
            targetFunctionsTable.background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
            return ValidationInfo(
                "Tick any methods to generate tests for", targetFunctionsTable.componentPopupMenu
            )
        }
        return null
    }
}