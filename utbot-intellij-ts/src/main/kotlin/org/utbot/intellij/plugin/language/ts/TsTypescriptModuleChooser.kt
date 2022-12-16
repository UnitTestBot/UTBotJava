package org.utbot.intellij.plugin.language.ts

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton

class TsTypescriptModuleChooser(val model: TsTestsModel) : TextFieldWithBrowseButton() {

    init {
        val descriptor = FileChooserDescriptor(
            true,
            false,
            false,
            false,
            false,
            false,
        )
        addBrowseFolderListener(
            TextBrowseFolderListener(descriptor, model.project)
        )
        text = "Specify path to \"typescript\" module"
    }
}