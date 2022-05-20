package org.utbot.intellij.plugin.ui.actions

import org.utbot.intellij.plugin.ui.UtTestsDialogProcessor
import org.utbot.intellij.plugin.ui.utils.PsiElementHandler
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils

class GenerateFromEditorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val element = findPsiElement(file, editor) ?: return

        val psiElementHandler = PsiElementHandler.makePsiElementHandler(file)

        if (psiElementHandler.isCreateTestActionAvailable(element)) {
            val srcClass = psiElementHandler.containingClass(element) ?: error("Containing class not found for element $element")
            val srcMethods = TestIntegrationUtils.extractClassMethods(srcClass, false)
            val focusedMethod = focusedMethodOrNull(element, srcMethods, psiElementHandler)

            UtTestsDialogProcessor.createDialogAndGenerateTests(project, setOf(srcClass), focusedMethod)
        }
    }

    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }

        return element
    }

    private fun focusedMethodOrNull(element: PsiElement, methods: List<MemberInfo>, psiElementHandler: PsiElementHandler): MemberInfo? {
        // getParentOfType might return element which does not correspond to the standard Psi hierarchy.
        // Thus, make transition to the Psi if it is required.
        val currentMethod = PsiTreeUtil.getParentOfType(element, psiElementHandler.methodClass)
            ?.let { psiElementHandler.toPsi(it, PsiMethod::class.java) }

        return methods.singleOrNull { it.member == currentMethod }
    }
}