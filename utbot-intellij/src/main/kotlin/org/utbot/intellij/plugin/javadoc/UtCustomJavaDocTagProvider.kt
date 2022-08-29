package org.utbot.intellij.plugin.javadoc

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue
import org.utbot.summary.comment.CustomJavaDocTag
import org.utbot.summary.comment.CustomJavaDocTagProvider

/**
 * Provides plugin's custom JavaDoc tags to make test summaries structured.
 */
class UtCustomJavaDocTagProvider : CustomJavadocTagProvider {
    override fun getSupportedTags(): List<UtCustomTagInfo> =
        CustomJavaDocTagProvider().getPluginCustomTags().map { UtCustomTagInfo(it) }

    class UtCustomTagInfo(private val tag: CustomJavaDocTag) : JavadocTagInfo {
        override fun getName(): String = tag.name

        fun getMessage(): String = tag.message

        override fun isInline() = false

        override fun checkTagValue(value: PsiDocTagValue?): String? = null

        override fun getReference(value: PsiDocTagValue?): PsiReference? = null

        override fun isValidInContext(element: PsiElement?): Boolean = element is PsiMethod
    }
}