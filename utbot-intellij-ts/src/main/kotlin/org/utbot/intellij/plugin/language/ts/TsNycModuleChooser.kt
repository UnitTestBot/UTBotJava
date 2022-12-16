package org.utbot.intellij.plugin.language.ts

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton

class TsNycModuleChooser(val model: TsTestsModel) : TextFieldWithBrowseButton() {

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
        text = "Specify path to \"@istanbuljs\\nyc-config-typescript\" module"
    }
}