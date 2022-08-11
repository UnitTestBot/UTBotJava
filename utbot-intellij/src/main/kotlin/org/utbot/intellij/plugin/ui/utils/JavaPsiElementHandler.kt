package org.utbot.intellij.plugin.ui.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testIntegration.createTest.CreateTestAction

class JavaPsiElementHandler(
    override val classClass: Class<PsiClass> = PsiClass::class.java,
    override val methodClass: Class<PsiMethod> = PsiMethod::class.java,
) : PsiElementHandler {
    /**
     * Casts element to clazz and returns the result.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> toPsi(element: PsiElement, clazz: Class<T>): T {
        return if (clazz.isInstance(element))
            element as? T ?: error("Cannot cast $element to $clazz")
        else error("Cannot cast $element to $clazz")
    }

    override fun isCreateTestActionAvailable(element: PsiElement): Boolean =
        CreateTestAction.isAvailableForElement(element)

    override fun containingClass(element: PsiElement): PsiClass? =
        PsiTreeUtil.getParentOfType(element, classClass, false)
}