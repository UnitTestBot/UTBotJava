package org.utbot.intellij.plugin.language.python

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.sdk.PythonSdkType
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant

object PythonLanguageAssistant : LanguageAssistant() {

    private const val pythonID = "Python"
    val language: Language = Language.findLanguageByID(pythonID) ?: error("Language wasn't found")

    data class Targets(
        val functions: Set<PyFunction>,
        val containingClass: PyClass?,
        val focusedFunction: PyFunction?,
        val file: PyFile,
        val editor: Editor?,
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (functions, containingClass, focusedFunction, file, editor) = getPsiTargets(e) ?: return

        PythonDialogProcessor.createDialogAndGenerateTests(
            project,
            functions,
            containingClass,
            focusedFunction,
            file,
            editor,
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !LockFile.isLocked() && getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Targets? {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return null

        if (file.module?.sdk?.sdkType !is PythonSdkType)
            return null

        val element = if (editor != null) {
            findPsiElement(file, editor) ?: return null
        } else {
            e.getData(CommonDataKeys.PSI_ELEMENT) ?: return null
        }

        val containingFunction = getContainingElement<PyFunction>(element)
        val containingClass = getContainingElement<PyClass>(element)

        if (containingClass == null) {
            val functions = file.topLevelFunctions
            if (functions.isEmpty())
                return null

            val focusedFunction = if (functions.contains(containingFunction)) containingFunction else null
            return Targets(functions.toSet(), null, focusedFunction, file, editor)
        }

        val functions = containingClass.methods
        if (functions.isEmpty())
            return null

        val focusedFunction =
            if (functions.any { it.name == containingFunction?.name }) containingFunction else null
        return Targets(functions.toSet(), containingClass, focusedFunction, file, editor)
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