package org.utbot.intellij.plugin.ui.components

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths
import org.utbot.common.PathUtil.replaceSeparator
import org.utbot.intellij.plugin.models.BaseTestsModel

class TestSourceDirectoryChooser(
    val model: BaseTestsModel,
    file: VirtualFile? = null
) : TextFieldWithBrowseButton() {
    private val projectRoot = file
        ?.let { getContentRoot(model.project, file) }
        ?: model.project.guessProjectDir()
        ?: error("Source file lies outside of a module")

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
        text = replaceSeparator(Paths.get(projectRoot.path, defaultDirectory).toString())
    }

    fun validatePath(): ValidationInfo? {
        val typedPath = Paths.get(text).toAbsolutePath()
        return if (typedPath.startsWith(replaceSeparator(projectRoot.path))) {
            defaultDirectory = Paths.get(projectRoot.path).relativize(typedPath).toString()
            null
        } else
            ValidationInfo("Specified directory lies outside of the project", this)
    }

    private fun getContentRoot(project: Project, file: VirtualFile): VirtualFile {
        return ProjectFileIndex.getInstance(project)
            .getContentRootForFile(file) ?: error("Source file lies outside of a module")
    }

    companion object {
        private var defaultDirectory = "utbot_tests"
    }
}