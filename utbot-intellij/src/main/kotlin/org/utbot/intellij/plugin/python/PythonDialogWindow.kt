package org.utbot.intellij.plugin.python

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.Panel
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.python.psi.*
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import com.jetbrains.python.refactoring.classes.ui.PyMemberSelectionTable
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

private const val SAME_PACKAGE_LABEL = "same as for sources"

class PythonDialogWindow(val model: PythonTestsModel): DialogWrapper(model.project) {

    private val functionsTable = PyMemberSelectionTable(emptyList(), null, false)

    private val testSourceFolderField = TestFolderComboWithBrowseButton(model)

    private val testPackageField = PackageNameReferenceEditorCombo(
        findTestPackageComboValue(),
        model.project,
        "org.utbot.recents",
        "Choose destination package"
    )

    private val testFrameworks = ComboBox(DefaultComboBoxModel(TestFramework.allItems.toTypedArray()))

    private lateinit var panel: DialogPanel

    @Suppress("UNCHECKED_CAST")
    private val itemsToHelpTooltip = hashMapOf(
        (testFrameworks as ComboBox<CodeGenerationSettingItem>) to ContextHelpLabel.create(""),
    )

    init {
        title = "Generate tests with UtBot"
        setResizable(false)
        init()
    }


    @Suppress("UNCHECKED_CAST")
    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                component(testSourceFolderField)
            }
            row("Test framework:") {
                makePanelWithHelpTooltip(
                    testFrameworks as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[testFrameworks]
                )
            }
            row("Destination package:") {
                component(testPackageField)
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

    private fun findTestPackageComboValue(): String {
        if (model.files.size != 1) {
            return SAME_PACKAGE_LABEL
        }
        val file = model.files.first()
        val path = file.virtualFile?.let { absoluteFilePath ->
            ProjectFileIndex.SERVICE.getInstance(model.project).getContentRootForFile(absoluteFilePath)?.let {absoluteProjectPath ->
                VfsUtil.getParentDir(VfsUtilCore.getRelativeLocation(absoluteFilePath, absoluteProjectPath))
            }
        } ?: SAME_PACKAGE_LABEL
        return path.replace('/', '.')
    }

    private fun updateFunctionsTable() {
        val items = model.fileMethods
        if (items != null) {
            updateMethodsTable(items)
            val height = functionsTable.rowHeight * (items.size.coerceAtMost(12) + 1)
            functionsTable.preferredScrollableViewportSize = JBUI.size(-1, height)
        }
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
}
