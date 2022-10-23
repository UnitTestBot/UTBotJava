package org.utbot.intellij.plugin.ui.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtFile

/**
 * Interface to abstract some checks and hierarchy actions from working with Java or Kotlin.
 *
 * Used in [org.utbot.intellij.plugin.ui.actions.GenerateTestsAction].
 */
interface PsiElementHandler {
    companion object {
        fun makePsiElementHandler(file: PsiFile): PsiElementHandler =
            when (file) {
                is KtFile -> KotlinPsiElementHandler()
                else -> JavaPsiElementHandler()
            }
    }
    /**
     * Check if the action to create tests is available for the provided PsiElement.
     */
    fun isCreateTestActionAvailable(element: PsiElement): Boolean

    /**
     * Get the containing PsiClass for the PsiElement.
     */
    fun containingClass(element: PsiElement): PsiClass?

    /**
     * Cast PsiElement to the provided class.
     *
     * It is required to abstract transition from other syntax trees(Kt tree) to Psi tree.
     * For instance, we can't cast KtNamedFunction to PsiMethod, but we can transition it.
     */
    fun <T> toPsi(element: PsiElement, clazz: Class<T>): T

    /**
     * Returns all classes that are declared in the [psiFile]
     */
    fun getClassesFromFile(psiFile: PsiFile): List<PsiClass> {
        return PsiTreeUtil.getChildrenOfTypeAsList(psiFile, classClass)
            .map { toPsi(it, PsiClass::class.java) }
    }

    /**
     * Get java class of the Class in the corresponding syntax tree (PsiClass, KtClass, e.t.c).
     */
    val classClass: Class<out PsiElement>

    /**
     * Get java class of the Method in the corresponding syntax tree (PsiMethod, KtNamedFunction, e.t.c).
     */
    val methodClass: Class<out PsiElement>
}