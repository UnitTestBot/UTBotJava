package org.utbot.intellij.plugin.javadoc

import com.intellij.codeInsight.javadoc.JavaDocExternalFilter
import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiJavaDocumentedElement

/**
 * To render UtBot custom JavaDoc tags messages, we need to override basic behaviour of [JavaDocumentationProvider].
 * The IJ platform knows only custom tag names, so we need to add their messages in rendered comments to make it look nice.
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
        val javaDocInfoWithUtSections =
            utJavaDocInfoGenerator.addUtBotSpecificSectionsToJavaDoc(baseJavaDocInfo, docComment)

        return JavaDocExternalFilter.filterInternalDocInfo(javaDocInfoWithUtSections)
    }

    /**
     * Replaces names of plugin's custom JavaDoc tags with their messages in the comment generated by the IJ platform.
     * Example: utbot.MethodUnderTest -> Method under test.
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