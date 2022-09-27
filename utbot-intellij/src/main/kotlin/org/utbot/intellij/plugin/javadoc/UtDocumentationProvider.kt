package org.utbot.intellij.plugin.javadoc

import com.intellij.codeInsight.javadoc.JavaDocExternalFilter
import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiJavaDocumentedElement

/**
 * To render UtBot custom JavaDoc tags messages, we need to override basic behaviour of [JavaDocumentationProvider].
 */
class UtDocumentationProvider : JavaDocumentationProvider() {

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        val target = comment.owner ?: comment

        if (target !is PsiJavaDocumentedElement) {
            return ""
        }

        val docComment = target.docComment ?: return ""

        val baseJavaDocInfoGenerator = JavaDocInfoGenerator(target.project, target)

        // get JavaDoc comment rendered by the platform.
        val baseJavaDocInfo = baseJavaDocInfoGenerator.generateRenderedDocInfo()

        // add UTBot sections with custom tags.
        val utJavaDocInfoGenerator = UtJavaDocInfoGenerator()

        if (baseJavaDocInfo.isNullOrEmpty()) {
            val javaDocInfoWithUtSections =
                utJavaDocInfoGenerator.addUtBotSpecificSectionsToJavaDoc(baseJavaDocInfo, docComment)
            return JavaDocExternalFilter.filterInternalDocInfo(javaDocInfoWithUtSections)
        }

        return JavaDocExternalFilter.filterInternalDocInfo(baseJavaDocInfo)
    }

    /**
     * Replaces names of plugin's custom JavaDoc tags with their messages in the comment generated by the IJ platform.
     * Example: utbot.methodUnderTest -> Method under test.
     *
     * Use it to update comment built by the IJ platform after updating to 2022.2.
     */
    private fun replaceTagNamesWithMessages(comment: String?) =
        comment?.let {
            val docTagProvider = UtCustomJavaDocTagProvider()
            docTagProvider.supportedTags.fold(it) { result, tag ->
                if (result.contains(tag.name)) {
                    result.replace(tag.name, "${tag.getMessage()}:")
                } else {
                    result
                }
            }
        } ?: ""
}