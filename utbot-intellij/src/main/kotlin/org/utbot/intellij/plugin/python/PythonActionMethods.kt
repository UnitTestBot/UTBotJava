package org.utbot.intellij.plugin.python

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.*

object PythonActionMethods {
    const val pythonID = "Python"

    fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (functions, focusedFunction) = getPsiTargets(e) ?: return

         PythonDialogProcessor.createDialogAndGenerateTests(
             project,
             functions,
             focusedFunction
         )
    }

    fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Pair<Set<PyFunction>, PyFunction?>? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return null
        val element = findPsiElement(file, editor) ?: return null
        val functions = file.topLevelFunctions

        if (functions.isEmpty())
            return null

        val containingFunction = getContainingFunction(element)
        val focusedFunction = if (functions.contains(containingFunction)) containingFunction else null

        return Pair(functions.toSet(), focusedFunction)
    }

    private fun getContainingFunction(element: PsiElement): PyFunction? {
        if (element is PyFunction)
            return element

        val parent = element.parent ?: return null
        return getContainingFunction(parent)
    }

    // this method is copy-paste from GenerateTestsActions.kt
    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }

        return element
    }
}