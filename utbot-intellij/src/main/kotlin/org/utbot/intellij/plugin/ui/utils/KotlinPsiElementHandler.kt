package org.utbot.intellij.plugin.ui.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.testIntegration.KotlinCreateTestIntention
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.uast.toUElement

class KotlinPsiElementHandler(
    // TODO: KtClassOrObject?
    override val classClass: Class<KtClass> = KtClass::class.java,
    override val methodClass: Class<KtNamedFunction> = KtNamedFunction::class.java,
) : PsiElementHandler {
    /**
     * Makes a transition from Kt to UAST and then to Psi.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> toPsi(element: PsiElement, clazz: Class<T>): T {
        return element.toUElement()?.javaPsi as? T ?: error("Could not cast $element to $clazz")
    }

    override fun isCreateTestActionAvailable(element: PsiElement): Boolean =
        getTarget(element)?.let { KotlinCreateTestIntention().applicabilityRange(it) != null } ?: false

    private fun getTarget(element: PsiElement?): KtNamedDeclaration? =
        element?.parentsWithSelf
            ?.firstOrNull { it is KtClassOrObject || it is KtNamedDeclaration && it.parent is KtFile } as? KtNamedDeclaration

    override fun containingClass(element: PsiElement): PsiClass? =
         element.parentsWithSelf.firstOrNull { it is KtClassOrObject }?.let { toPsi(it, PsiClass::class.java) }
}