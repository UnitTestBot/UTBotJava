package org.utbot.intellij.plugin.language.js

import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.Cell
import com.intellij.util.ui.JBUI
import framework.codegen.Mocha
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.intellij.plugin.ui.components.TestSourceDirectoryChooser
import settings.JsTestGenerationSettings.defaultTimeout
import java.io.File
import java.nio.file.Paths
import javax.swing.JComboBox
import javax.swing.JComponent

class JsDialogWindow(val model: JsTestsModel) : DialogWrapper(model.project) {

    private val items = model.fileMethods

    private val functionsTable = JSMemberSelectionTable(items, null, null).apply {
        val height = this.rowHeight * (items.size.coerceAtMost(12) + 1)
        this.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

    private val testSourceFolderField = TestSourceDirectoryChooser(model, model.file.virtualFile)
    private val testFrameworks = listOf<TestFramework>(Mocha)
    private val nycSourceFileChooserField = NycSourceFileChooser(model)
    private val coverageMode = CoverageModeButtons

    private lateinit var panel: DialogPanel

    private val timeoutSpinner =
        JBIntSpinner(
            defaultTimeout.toInt(),
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS,
            Int.MAX_VALUE,
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS
        )

    init {
        title = "Generate Tests with UtBot"
        super.setOKButtonText("Generate Tests")
        isResizable = false
        init()
    }


    @Suppress("UNCHECKED_CAST")
    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                cell(testSourceFolderField)
            }
            row("Test framework:") {
                comboBox(testFrameworks, null)
            }
            row("Nyc source path:") {
                cell(nycSourceFileChooserField)
            }
            row("Coverage mode:") {
                cell(coverageMode.fastButton).apply { this.component.isSelected == true }
                cell(coverageMode.baseButton)
//                panelWithHelpTooltip("Fast mode can't find timeouts, but works faster") {
//                    component(coverageMode.fastButton, coverageMode.baseButton)
//                    component(coverageMode.baseButton)
//                }
            }
            row("Timeout for Node.js (in seconds):") {
                cell(timeoutSpinner)
            }
            row("Generate test methods for:") {}
            row {
                scrollCell(functionsTable)
            }
        }
        updateMembersTable()
        setListeners()
        return panel
    }


    private inline fun Cell.panelWithHelpTooltip(tooltipText: String?, crossinline init: Cell.() -> Unit): Cell {
        init()
        tooltipText?.let { component(ContextHelpLabel.create(it)) }
        return this
    }


    override fun doOKAction() {
        val selected = functionsTable.selectedMemberInfos.toSet()
        model.selectedMethods = if (selected.any()) selected else emptySet()
        model.testFramework = testFrameworks
        model.timeout = timeoutSpinner.number.toLong()
        model.pathToNYC = nycSourceFileChooserField.text
        model.coverageMode = coverageMode.mode
        File(testSourceFolderField.text).mkdir()
        model.testSourceRoot =
            VirtualFileManager.getInstance().refreshAndFindFileByNioPath(Paths.get(testSourceFolderField.text))
        super.doOKAction()
    }

    override fun doValidate(): ValidationInfo? {
        return testSourceFolderField.validatePath() ?: nycSourceFileChooserField.validateNyc()
    }

    private fun updateMembersTable() {
        if (items.isEmpty()) isOKActionEnabled = false
        val focusedNames = model.selectedMethods.map { it.member.name }
        val selectedMethods = items.filter {
            focusedNames.contains(it.member.name)
        }
        if (selectedMethods.isEmpty()) {
            checkMembers(items)
        } else {
            checkMembers(selectedMethods)
        }
    }

    @Suppress("unused")
    private fun configureTestFrameworkIfRequired() {
//        initTestFrameworkPresenceThread.join()
        val frameworkNotInstalled = !testFrameworks.item.isInstalled
        if (frameworkNotInstalled) {
            Messages.showErrorDialog(
                "Test framework ${testFrameworks.item.displayName} is not installed. " +
                        "Run \"npm i -g ${testFrameworks.item.displayName}\".",
                "Missing Framework"
            )
        }
    }


    private fun setListeners() {

        testSourceFolderField.childComponent.addActionListener { event ->
            with((event.source as JComboBox<*>).selectedItem) {
                if (this is VirtualFile) {
                    model.setSourceRootAndFindTestModule(this@with)
                } else {
                    model.setSourceRootAndFindTestModule(null)
                }
            }
        }
    }

    private fun checkMembers(members: Collection<JSMemberInfo>) = members.forEach { it.isChecked = true }


}

private const val MINIMUM_TIMEOUT_VALUE_IN_SECONDS = 1
