package org.utbot.intellij.plugin.language.js

import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreterManager
import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.Panel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import framework.codegen.JsCgLanguageAssistant
import framework.codegen.Mocha
import java.awt.BorderLayout
import java.io.File
import java.nio.file.Paths
import java.util.Locale
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.intellij.plugin.ui.components.TestSourceDirectoryChooser
import settings.JsTestGenerationSettings.defaultTimeout
import kotlin.concurrent.thread

class JsDialogWindow(val model: JsTestsModel) : DialogWrapper(model.project) {

    private val items = model.fileMethods

    private val functionsTable = JSMemberSelectionTable(items, null, null).apply {
        val height = this.rowHeight * (items.size.coerceAtMost(12) + 1)
        this.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

    private fun findTestPackageComboValue() = SAME_PACKAGE_LABEL

    private val nodeInterp = try {
        NodeJsLocalInterpreterManager.getInstance().interpreters.first()
    } catch (e: NoSuchElementException) {
        throw IllegalStateException("Node.js interpreter is not set in the IDEA settings!")
    }

    private val cbSpecifyTestPackage = CheckBox("Specify destination package", false)
    private val testPackageField = PackageNameReferenceEditorCombo(
        findTestPackageComboValue(),
        model.project,
        RECENTS_KEY,
        "Choose Destination Package"
    )

    private val testSourceFolderField = TestSourceDirectoryChooser(model, model.file.virtualFile)
    private val testFrameworks = ComboBox(DefaultComboBoxModel(arrayOf(Mocha)))
    private val nycSourceFileChooserField = NycSourceFileChooser(model)

    private var initTestFrameworkPresenceThread: Thread
    private lateinit var panel: DialogPanel

    private val timeoutSpinner =
        JBIntSpinner(
            defaultTimeout.toInt(),
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS,
            Int.MAX_VALUE,
            MINIMUM_TIMEOUT_VALUE_IN_SECONDS
        )

    init {
        model.pathToNode = nodeInterp.interpreterSystemDependentPath.replace("\\", "/")
        model.pathToNPM = "\"${model.pathToNode.substringBeforeLast("/") + "/" + "npm"}\""
        //TODO: fix.
        model.pathToNode = "node"
        title = "Generate Tests with UtBot"
        initTestFrameworkPresenceThread = thread(start = true) {
            JsCgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks.forEach {
                it.isInstalled = findFrameworkLibrary(it.displayName.lowercase(Locale.getDefault()), model)
            }
        }
        isResizable = false
        init()
    }


    @Suppress("UNCHECKED_CAST")
    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                component(testSourceFolderField)
            }
            row("Test framework:") {
                component(
                    Panel().apply {
                        add(testFrameworks as ComboBox<CodeGenerationSettingItem>, BorderLayout.LINE_START)
                    }
                )
            }
            row("Nyc source path:") {
                component(nycSourceFileChooserField)
            }
            row("Timeout for Node.js (in seconds):") {
                panelWithHelpTooltip("The execution timeout") {
                    component(timeoutSpinner)
                    component(JBLabel("sec"))
                }
            }
            row {
                component(cbSpecifyTestPackage)
            }.apply { visible = false }
            row("Destination package:") {
                component(testPackageField)
            }.apply { visible = false }
            row("Generate test methods for:") {}
            row {
                scrollPane(functionsTable)
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
        model.testPackageName =
            if (testPackageField.text != SAME_PACKAGE_LABEL) testPackageField.text else ""
        val selected = functionsTable.selectedMemberInfos.toSet()
        model.selectedMethods = if (selected.any()) selected else emptySet()
        model.testFramework = testFrameworks.item
        model.timeout = timeoutSpinner.number.toLong()
        model.pathToNYC = nycSourceFileChooserField.text
        File(testSourceFolderField.text).mkdir()
        model.testSourceRoot = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(Paths.get(testSourceFolderField.text))
        configureTestFrameworkIfRequired()
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

    private fun configureTestFrameworkIfRequired() {
        initTestFrameworkPresenceThread.join()
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

private const val RECENTS_KEY = "org.utbot.recents"
private const val SAME_PACKAGE_LABEL = "same as for sources"
private const val MINIMUM_TIMEOUT_VALUE_IN_SECONDS = 1
