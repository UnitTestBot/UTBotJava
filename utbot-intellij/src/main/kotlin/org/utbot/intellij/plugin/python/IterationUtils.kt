package org.utbot.intellij.plugin.python

import com.intellij.psi.PsiElement

object IterationUtils {
    inline fun <reified T: PsiElement> getContainingElement(element: PsiElement): T? {
        var result = element
        while (result !is T && (result.parent != null)) {
            result = result.parent
        }
        return result as? T
    }
}