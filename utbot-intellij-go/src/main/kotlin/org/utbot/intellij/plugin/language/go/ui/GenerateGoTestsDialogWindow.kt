package org.utbot.intellij.plugin.language.go.ui

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.refactor.ui.GoDeclarationInfo
import com.goide.sdk.combobox.GoSdkChooserCombo
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.utbot.go.logic.GoUtTestsGenerationConfig
import org.utbot.intellij.plugin.language.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.language.go.ui.utils.resolveGoExecutablePath
import java.text.ParseException
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

private const val MINIMUM_EACH_EXECUTION_TIMEOUT_MILLIS = 1
private const val EACH_EXECUTION_TIMEOUT_MILLIS_SPINNER_STEP = 10

private const val MINIMUM_ALL_EXECUTION_TIMEOUT_SECONDS = 1
private const val ALL_EXECUTION_TIMEOUT_SECONDS_SPINNER_STEP = 10

// This class is highly inspired by GenerateTestsDialogWindow.
class GenerateGoTestsDialogWindow(val model: GenerateGoTestsModel) : DialogWrapper(model.project) {

    private val targetInfos = model.targetFunctions.toInfos()
    private val targetFunctionsTable = GoFunctionsSelectionTable(targetInfos).apply {
        val height = this.rowHeight * (targetInfos.size.coerceAtMost(12) + 1)
        this.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

    private val projectGoSdkField = GoSdkChooserCombo()
    private val allFunctionExecutionTimeoutSecondsSpinner =
        JBIntSpinner(
            TimeUnit.MILLISECONDS.toSeconds(GoUtTestsGenerationConfig.DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS).toInt(),
            MINIMUM_ALL_EXECUTION_TIMEOUT_SECONDS,
            Int.MAX_VALUE,
            ALL_EXECUTION_TIMEOUT_SECONDS_SPINNER_STEP
        )
    private val eachFunctionExecutionTimeoutMillisSpinner =
        JBIntSpinner(
            GoUtTestsGenerationConfig.DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS.toInt(),
            MINIMUM_EACH_EXECUTION_TIMEOUT_MILLIS,
            Int.MAX_VALUE,
            EACH_EXECUTION_TIMEOUT_MILLIS_SPINNER_STEP
        )

    private lateinit var panel: DialogPanel

    init {
        title = "Generate Tests with UtBot"
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root: near to source files") {}
            row("Project Go SDK:") {
                component(projectGoSdkField)
            }
            row("Generate test methods for:") {}
            row {
                scrollPane(targetFunctionsTable)
            }
            row("Timeout for all functions:") {
                component(allFunctionExecutionTimeoutSecondsSpinner)
                component(JBLabel("seconds"))
            }
            row("Timeout for each function execution:") {
                component(eachFunctionExecutionTimeoutMillisSpinner)
                component(JBLabel("ms"))
            }
        }
        updateFunctionsOrMethodsTable()
        return panel
    }

    override fun doOKAction() {
        model.selectedFunctions = targetFunctionsTable.selectedMemberInfos.fromInfos()
        model.goExecutableAbsolutePath = projectGoSdkField.sdk.resolveGoExecutablePath()!!
        try {
            eachFunctionExecutionTimeoutMillisSpinner.commitEdit()
            allFunctionExecutionTimeoutSecondsSpinner.commitEdit()
        } catch (_: ParseException) {
        }
        model.eachFunctionExecutionTimeoutMillis = eachFunctionExecutionTimeoutMillisSpinner.number.toLong()
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
        projectGoSdkField.sdk.resolveGoExecutablePath()
            ?: return ValidationInfo(
                "Go SDK is not configured",
                projectGoSdkField.childComponent
            )

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