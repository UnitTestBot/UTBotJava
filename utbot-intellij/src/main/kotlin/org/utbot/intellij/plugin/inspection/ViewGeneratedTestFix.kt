package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.EmptyIcon
import javax.swing.Icon
import org.utbot.common.PathUtil.toPath

/**
 * Button with a link to the [testFileRelativePath]. Displayed as a quick fix.
 *
 * @param testFileRelativePath path to the generated test file.
 *                             Should be relative to the project root
 * @param lineNumber one-based line number
 * @param columnNumber one-based column number
 */
class ViewGeneratedTestFix(
    val testFileRelativePath: String,
    val lineNumber: Int,
    val columnNumber: Int
) : LocalQuickFix, Iconable {

    /**
     * Navigates the user to the [lineNumber] line of the [testFileRelativePath] file.
     */
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val testFileAbsolutePath = project.basePath?.toPath()?.resolve(testFileRelativePath) ?: return
        val virtualFile = VfsUtil.findFile(testFileAbsolutePath, /* refreshIfNeeded = */ true) ?: return
        val editor = FileEditorManager.getInstance(project)
        editor.openFile(virtualFile, /* focusEditor = */ true)
        val caretModel = editor.selectedTextEditor?.caretModel ?: return
        val zeroBasedPosition = LogicalPosition(lineNumber - 1, columnNumber - 1)
        caretModel.moveToLogicalPosition(zeroBasedPosition)
        val selectionModel = editor.selectedTextEditor?.selectionModel ?: return
        selectionModel.selectLineAtCaret()
    }

    /**
     * This text is displayed on the quick fix button.
     */
    override fun getName() = "View generated test"

    override fun getFamilyName() = name

    override fun getIcon(flags: Int): Icon = EmptyIcon.ICON_0
}