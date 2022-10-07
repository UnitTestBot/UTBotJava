package org.utbot.intellij.plugin.language.go

import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoMethodDeclaration
import com.goide.psi.GoPointerType
import com.goide.psi.GoStructType
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant
import org.utbot.intellij.plugin.language.go.generator.GoUtTestsDialogProcessor

@Suppress("unused") // is used in org.utbot.intellij.plugin.language.agnostic.LanguageAssistant via reflection
object GoLanguageAssistant : LanguageAssistant() {

    private const val goId = "go"
    val language: Language = Language.findLanguageByID(goId) ?: error("Go language wasn't found")

    private data class PsiTargets(
        val targetFunctions: Set<GoFunctionOrMethodDeclaration>,
        val focusedTargetFunctions: Set<GoFunctionOrMethodDeclaration>,
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (targetFunctions, focusedTargetFunctions) = getPsiTargets(e) ?: return
        GoUtTestsDialogProcessor.createDialogAndGenerateTests(
            project,
            targetFunctions,
            focusedTargetFunctions
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): PsiTargets? {
        e.project ?: return null

        // The action is being called from editor or return. TODO: support other cases instead of return.
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null

        val file = e.getData(CommonDataKeys.PSI_FILE) as? GoFile ?: return null
        val element = findPsiElement(file, editor) ?: return null

        val containingFunction = getContainingFunction(element)
        val targetFunctions = extractTargetFunctionsOrMethods(file)
        val focusedTargetFunctions = if (containingFunction != null) {
            setOf(containingFunction)
        } else {
            emptySet()
        }

        return PsiTargets(targetFunctions, focusedTargetFunctions)
    }

    // TODO: logic can be modified. For example, maybe suggest methods of the containing struct if present.
    private fun extractTargetFunctionsOrMethods(file: GoFile): Set<GoFunctionOrMethodDeclaration> {
        return file.functions.toSet() union file.methods.toSet()
    }

    private fun getContainingFunction(element: PsiElement): GoFunctionOrMethodDeclaration? {
        if (element is GoFunctionOrMethodDeclaration)
            return element

        val parent = element.parent ?: return null
        return getContainingFunction(parent)
    }

    // Unused for now, but may be used for more complicated extract logic in the future.
    @Suppress("unused")
    private fun getContainingStruct(element: PsiElement): GoStructType? =
        PsiTreeUtil.getParentOfType(element, GoStructType::class.java, false)

    // Unused for now, but may be used to access all methods of receiver's struct.
    @Suppress("unused")
    private fun getMethodReceiverStruct(method: GoMethodDeclaration): GoStructType {
        val receiverType = method.receiverType
        if (receiverType is GoPointerType) {
            return receiverType.type as GoStructType
        }
        return receiverType as GoStructType
    }

    // This method is cloned from GenerateTestsActions.kt.
    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }

        return element
    }
}