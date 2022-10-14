package org.utbot.intellij.plugin.language.js

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import settings.JsDynamicSettings


class NycSourceFileChooser(val model: JsTestsModel) : TextFieldWithBrowseButton() {


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
        text = getFrameworkLibraryPath(JsDynamicSettings().pathToNYC, model) ?: "Nyc does not found"
    }
}
