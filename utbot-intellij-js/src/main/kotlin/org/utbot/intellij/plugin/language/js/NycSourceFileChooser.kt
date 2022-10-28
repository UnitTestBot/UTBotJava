package org.utbot.intellij.plugin.language.js

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import org.utbot.common.PathUtil.replaceSeparator
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
        text = replaceSeparator(getFrameworkLibraryPath(JsDynamicSettings().pathToNYC, model) ?: "Nyc was not found")
    }

    fun validateNyc(): ValidationInfo? {
        return if (replaceSeparator(text).endsWith("nyc"))
            null
        else
            ValidationInfo("Nyc executable file was not found in the specified directory", this)
    }
}
