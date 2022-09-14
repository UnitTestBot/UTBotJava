package org.utbot.intellij.plugin.javadoc

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.codeInsight.javadoc.JavaDocUtil
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import mu.KotlinLogging

private const val LINK_TAG = "link"
private const val LINKPLAIN_TAG = "linkplain"
private const val LITERAL_TAG = "literal"
private const val CODE_TAG = "code"
private const val SYSTEM_PROPERTY_TAG = "systemProperty"
private const val MESSAGE_SEPARATOR = ":"
private const val PARAGRAPH_TAG = "<p>"
private const val CODE_TAG_START = "<code>"
private const val CODE_TAG_END = "</code>"

private val logger = KotlinLogging.logger {}

/**
 * Generates UtBot specific sections to include them to rendered JavaDoc comment.
 *
 * Methods responsible for value generation were taken from IJ platform class (they are private and couldn't be used outside).
 *
 * See [com.intellij.codeInsight.javadoc.JavaDocInfoGenerator].
 *
 * It wouldn't be needed to generate rendered doc on our own after updating to the IJ platform 2022.2,
 * so delete it after updating and use basic [com.intellij.codeInsight.javadoc.JavaDocInfoGenerator].
 */
class UtJavaDocInfoGenerator {
    fun addUtBotSpecificSectionsToJavaDoc(javadoc: String?, comment: PsiDocComment): String {
        val builder = if (javadoc == null) {
            StringBuilder()
        } else {
            StringBuilder(javadoc)
        }

        val docTagProvider = UtCustomJavaDocTagProvider()
        docTagProvider.supportedTags.forEach {
            generateUtTagSection(builder, comment, it)
        }
        return builder.toString()
    }

    /**
     * Searches for UtBot tag in the comment and generates a related section for it.
     */
    private fun generateUtTagSection(
        builder: StringBuilder,
        comment: PsiDocComment,
        utTag: UtCustomJavaDocTagProvider.UtCustomTagInfo
    ) {
        val tag = comment.findTagByName(utTag.name) ?: return
        startHeaderSection(builder, utTag.getMessage()).append(PARAGRAPH_TAG)
        val sectionContent = buildString {
            generateValue(this, tag.dataElements)
            trim()
        }

        builder.append(sectionContent)
        builder.append(DocumentationMarkup.SECTION_END)
    }

    private fun startHeaderSection(builder: StringBuilder, message: String): StringBuilder =
        builder.append(DocumentationMarkup.SECTION_HEADER_START)
            .append(message)
            .append(MESSAGE_SEPARATOR)
            .append(DocumentationMarkup.SECTION_SEPARATOR)

    /**
     * Generates info depending on tag's value type.
     */
    private fun generateValue(builder: StringBuilder, elements: Array<PsiElement>) {
        if (elements.isEmpty()) {
            return
        }

        var offset = elements[0].textOffset + elements[0].text.length

        for (element in elements) {
            with(element) {
                if (textOffset > offset) {
                    builder.append(' ')
                }

                offset = textOffset + text.length

                if (element is PsiInlineDocTag) {
                    when (element.name) {
                        LITERAL_TAG -> generateLiteralValue(builder, element)
                        CODE_TAG, SYSTEM_PROPERTY_TAG -> generateCodeValue(element, builder)
                        LINK_TAG -> generateLinkValue(element, builder, false)
                        LINKPLAIN_TAG -> generateLinkValue(element, builder, true)
                    }
                } else {
                    appendPlainText(builder, text)
                }
            }
        }
    }

    private fun appendPlainText(builder: StringBuilder, text: String) {
        builder.append(StringUtil.replaceUnicodeEscapeSequences(text))
    }

    private fun collectElementText(builder: StringBuilder, element: PsiElement) {
        element.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is PsiWhiteSpace ||
                    element is PsiJavaToken ||
                    element is PsiDocToken && element.tokenType !== JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS
                ) {
                    builder.append(element.text)
                }
            }
        })
    }

    private fun generateCodeValue(tag: PsiInlineDocTag, builder: StringBuilder) {
        builder.append(CODE_TAG_START)
        val pos = builder.length
        generateLiteralValue(builder, tag)
        builder.append(CODE_TAG_END)
        if (builder[pos] == '\n') {
            builder.insert(
                pos,
                ' '
            ) // line break immediately after opening tag is ignored by JEditorPane
        }
    }

    private fun generateLiteralValue(builder: StringBuilder, tag: PsiDocTag) {
        val literalValue = buildString {
            val children = tag.children
            for (i in 2 until children.size - 1) { // process all children except tag opening/closing elements
                val child = children[i]
                if (child is PsiDocToken && child.tokenType === JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS) {
                    continue
                }

                var elementText = child.text
                if (child is PsiWhiteSpace) {
                    val pos = elementText.lastIndexOf('\n')
                    if (pos >= 0) {
                        elementText = elementText.substring(0, pos + 1) // skip whitespace before leading asterisk
                    }
                }
                appendPlainText(this, StringUtil.escapeXmlEntities(elementText))
            }
        }
        builder.append(StringUtil.trimLeading(literalValue))
    }

    private fun generateLinkValue(tag: PsiInlineDocTag, builder: StringBuilder, plainLink: Boolean) {
        val tagElements = tag.dataElements
        val linkText = createLinkText(tagElements)
        if (linkText.isNotEmpty()) {
            val index = JavaDocUtil.extractReference(linkText)
            val referenceText = linkText.substring(0, index).trim()
            val label = StringUtil.nullize(linkText.substring(index).trim())
            generateLink(builder, referenceText, label, tagElements[0], plainLink)
        }
    }

    private fun createLinkText(tagElements: Array<PsiElement>): String {
        var offset = if (tagElements.isNotEmpty()) {
            tagElements[0].textOffset + tagElements[0].text.length
        } else {
            0
        }

        return buildString {
            for (i in tagElements.indices) {
                val tagElement = tagElements[i]
                if (tagElement.textOffset > offset) {
                    this.append(' ')
                }
                offset = tagElement.textOffset + tagElement.text.length
                collectElementText(this, tagElement)
                if (i < tagElements.lastIndex) {
                    this.append(' ')
                }
            }
        }.trim()
    }

    private fun generateLink(
        builder: StringBuilder,
        refText: String?,
        label: String?,
        context: PsiElement,
        plainLink: Boolean
    ) {
        val linkLabel = label ?: context.manager.let {
            JavaDocUtil.getLabelText(it.project, it, refText, context)
        }

        var target: PsiElement? = null
        try {
            if (refText != null) {
                target = JavaDocUtil.findReferenceTarget(context.manager, refText, context)
            }
        } catch (e: IndexNotReadyException) {
            logger.info(e) { "Failed to find a reference while generating JavaDoc comment. Details: ${e.message}" }
        }

        if (target == null && DumbService.isDumb(context.project)) {
            builder.append(linkLabel)
        } else if (target == null) {
            builder.append("<font color=red>").append(linkLabel).append("</font>")
        } else {
            JavaDocUtil.getReferenceText(target.project, target)?.let {
                DocumentationManagerUtil.createHyperlink(builder, target, it, linkLabel, plainLink)
            }
        }
    }
}