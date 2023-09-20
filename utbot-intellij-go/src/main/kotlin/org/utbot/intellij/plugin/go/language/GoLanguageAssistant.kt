package org.utbot.intellij.plugin.go.language

import com.goide.psi.*
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant
import org.utbot.intellij.plugin.go.generator.GoUtTestsDialogProcessor

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
        val file = e.getData(CommonDataKeys.PSI_FILE) as? GoFile ?: return
        val module = ModuleUtilCore.findModuleForFile(file) ?: return
        val (targetFunctions, focusedTargetFunctions) = getPsiTargets(e) ?: return
        GoUtTestsDialogProcessor.createDialogAndGenerateTests(
            project,
            module,
            targetFunctions,
            focusedTargetFunctions
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): PsiTargets? {
        e.project ?: return null

        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE) as? GoFile ?: return null
        val element = if (editor != null) {
            findPsiElement(file, editor) ?: return null
        } else {
            e.getData(CommonDataKeys.PSI_ELEMENT) ?: return null
        }

        val targetFunctions = extractTargetFunctionsOrMethods(file)
        val containingFunctionOrMethod = getContainingFunctionOrMethod(element)
        val containingStruct = getContainingStruct(element)
        val focusedTargetFunctions = if (containingFunctionOrMethod != null) {
            setOf(containingFunctionOrMethod)
        } else {
            if (containingStruct != null) {
                targetFunctions.filterIsInstance<GoMethodDeclaration>()
                    .filter { containingStruct == getMethodReceiverStruct(it) }.toSet()
            } else {
                emptySet()
            }
        }

        return PsiTargets(targetFunctions, focusedTargetFunctions)
    }

    private fun extractTargetFunctionsOrMethods(file: GoFile): Set<GoFunctionOrMethodDeclaration> {
        return file.functions.toSet() union file.methods.toSet()
    }

    private fun getContainingFunctionOrMethod(element: PsiElement): GoFunctionOrMethodDeclaration? {
        if (element is GoFunctionOrMethodDeclaration)
            return element

        val parent = element.parent ?: return null
        return getContainingFunctionOrMethod(parent)
    }

    private fun getContainingStruct(element: PsiElement): GoStructType? =
        PsiTreeUtil.getParentOfType(element, GoStructType::class.java, false)

    private fun getMethodReceiverStruct(method: GoMethodDeclaration): GoStructType? {
        val receiverType = method.receiverType?.contextlessUnderlyingType ?: return null
        if (receiverType is GoPointerType) {
            return receiverType.type?.contextlessUnderlyingType as? GoStructType
        }
        return receiverType as? GoStructType
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