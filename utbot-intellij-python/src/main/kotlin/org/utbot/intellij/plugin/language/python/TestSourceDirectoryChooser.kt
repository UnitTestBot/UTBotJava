package org.utbot.intellij.plugin.language.python

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import java.nio.file.Paths

class TestSourceDirectoryChooser(
    val model: PythonTestsModel
) : TextFieldWithBrowseButton() {
    private val projectRoot = getContentRoot(model.project, model.file.virtualFile)

    init {
        val descriptor = FileChooserDescriptor(
            false,
            true,
            false,
            false,
            false,
            false
        )
        descriptor.setRoots(projectRoot)
        addBrowseFolderListener(
            TextBrowseFolderListener(descriptor, model.project)
        )
        text = Paths.get(projectRoot.path, defaultDirectory).toString()
    }

    fun validatePath(): ValidationInfo? {
        val typedPath = Paths.get(text).toAbsolutePath()
        return if (typedPath.startsWith(projectRoot.path)) {
            defaultDirectory = Paths.get(projectRoot.path).relativize(typedPath).toString()
            null
        } else
            ValidationInfo("Specified directory lies outside of the project", this)
    }

    companion object {
        private var defaultDirectory = "utbot_tests"
    }
}