package org.utbot.intellij.plugin.javadoc

import com.intellij.codeInsight.javadoc.JavaDocExternalFilter
import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.javadoc.PsiDocComment

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

        return getRenderedDoc(baseJavaDocInfo, docComment, comment)
    }

    /**
     * Processes JavaDoc generated by IJ platform to render plugin's custom tags correctly.
     */
    private fun getRenderedDoc(
        baseJavaDocInfo: String?,
        docComment: PsiDocComment,
        comment: PsiDocCommentBase
    ): String? {
        val utJavaDocInfoGenerator = UtJavaDocInfoGenerator()
        // case 1: IDE successfully parsed comment with plugin's custom tags,
        // and we only need to replace tags names with their messages.
        return if (baseJavaDocInfo != null && baseJavaDocInfo.contains("@utbot")) {
            val finalJavaDoc = replaceTagNamesWithMessages(baseJavaDocInfo)
            JavaDocExternalFilter.filterInternalDocInfo(finalJavaDoc)
            // case 2: IDE failed to parse plugin's tags, and we need to add them on our own.
        } else if (baseJavaDocInfo != null && comment.text.contains("@utbot")) {
            val javaDocInfoWithUtSections =
                utJavaDocInfoGenerator.addUtBotSpecificSectionsToJavaDoc(docComment)
            val finalJavaDoc = replaceTagNamesWithMessages(javaDocInfoWithUtSections)
            JavaDocExternalFilter.filterInternalDocInfo(finalJavaDoc)
        } else {
            // case 3: comment doesn't contain plugin's tags, so IDE can parse it on its own.
            super.generateRenderedDoc(comment)
        }
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