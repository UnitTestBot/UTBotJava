package org.utbot.intellij.plugin.javadoc

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue

/**
 * Provides plugin's custom JavaDoc tags to make test summaries structured.
 */
class UtCustomJavaDocTagProvider : CustomJavadocTagProvider {
    override fun getSupportedTags(): List<JavadocTagInfo> =
        listOf(
            UtCustomTag.ClassUnderTest,
            UtCustomTag.MethodUnderTest,
            UtCustomTag.ExpectedResult,
            UtCustomTag.ActualResult,
            UtCustomTag.Executes,
            UtCustomTag.Invokes,
            UtCustomTag.ReturnsFrom,
            UtCustomTag.ThrowsException,
        )

    sealed class UtCustomTag(private val name: String) : JavadocTagInfo {
        override fun getName(): String = name

        override fun isInline() = false

        override fun checkTagValue(value: PsiDocTagValue?): String? = null

        override fun getReference(value: PsiDocTagValue?): PsiReference? = null

        override fun isValidInContext(element: PsiElement?): Boolean {
            return element is PsiMethod
        }

        object ClassUnderTest : UtCustomTag("utbot.classUnderTest")
        object MethodUnderTest : UtCustomTag("utbot.methodUnderTest")
        object ExpectedResult : UtCustomTag("utbot.expectedResult")
        object ActualResult : UtCustomTag("utbot.actualResult")
        object Executes : UtCustomTag("utbot.executes")
        object Invokes : UtCustomTag("utbot.invokes")
        object ReturnsFrom : UtCustomTag("utbot.returnsFrom")
        object ThrowsException : UtCustomTag("utbot.throwsException")
    }
}