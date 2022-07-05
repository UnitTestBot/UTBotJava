package org.utbot.intellij.plugin.python

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.ui.layout.panel
import javax.swing.JComponent

private const val RECENTS_KEY = "org.utbot.recents"

private const val SAME_PACKAGE_LABEL = "same as for sources"

class PythonDialogWindow(val model: PythonTestsModel): DialogWrapper(model.project) {

    private val testPackageField = PackageNameReferenceEditorCombo(
        findTestPackageComboValue(),
        model.project,
        RECENTS_KEY,
        "Choose destination package"
    )

//    private val functionsTable = PythonFunctionsSelectionTable(emptyList(), null)

//    private val testSourceFolderField = PythonTestFolderComboWithBrowseButton(model)

    private fun findTestPackageComboValue(): String {
        return SAME_PACKAGE_LABEL  // TODO("add path selector")
    }

    init {
        title = "Generate tests with UtBot"
        setResizable(false)
        init()
    }

    override fun createCenterPanel(): JComponent? {
        panel = panel {
//            row("Test source root:") {
//                component(testSourceFolderField)
//            }
            row("Destination package:") {
                component(testPackageField)
            }
            row("Generate test methods for:") {}
//            row {
//                scrollPane(functionsTable)
//            }
        }

        initDefaultValues()
        return panel
    }

    private fun initDefaultValues() {
        testPackageField.isEnabled = false
    }
    private fun setListeners() {
    }
}
