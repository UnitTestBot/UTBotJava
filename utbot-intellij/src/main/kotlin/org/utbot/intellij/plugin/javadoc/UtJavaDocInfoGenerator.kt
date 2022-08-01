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

private val logger = KotlinLogging.logger {}

class UtJavaDocInfoGenerator {
    /**
     * Generates UtBot specific sections to include them to rendered JavaDoc comment.
     */
    fun addUtBotSpecificSectionsToJavaDoc(javadoc: String?, comment: PsiDocComment): String {
        val builder: StringBuilder = StringBuilder(javadoc)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.ClassUnderTest)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.MethodUnderTest)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.Invokes)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.Executes)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.Iterates)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.ExpectedResult)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.ActualResult)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.ReturnsFrom)
        generateUtTagSection(builder, comment, UtCustomJavaDocTagProvider.UtCustomTag.ThrowsException)
        return builder.toString()
    }

    /**
     * Searches for UtBot tag in the comment and generates a related section for it.
     */
    private fun generateUtTagSection(
        builder: StringBuilder,
        comment: PsiDocComment?,
        utTag: UtCustomJavaDocTagProvider.UtCustomTag
    ) {
        if (comment != null) {
            val tag = comment.findTagByName(utTag.name) ?: return
            startHeaderSection(builder, utTag.getMessage())?.append("<p>")
            val sectionContent = buildString {
                generateValue(this, tag.dataElements)
                this.trim { it <= ' ' }
            }
            builder.append(sectionContent)
            builder.append(DocumentationMarkup.SECTION_END)
        }
    }

    private fun startHeaderSection(builder: StringBuilder, message: String): StringBuilder? {
        return builder.append(DocumentationMarkup.SECTION_HEADER_START)
            .append(message)
            .append(MESSAGE_SEPARATOR)
            .append(DocumentationMarkup.SECTION_SEPARATOR)
    }

    /**
     * Generates info depending on tag's value type.
     */
    private fun generateValue(builder: StringBuilder, elements: Array<PsiElement>) {
        var offset = if (elements.isNotEmpty()) {
            elements[0].textOffset + elements[0].text.length
        } else 0

        for (i in elements.indices) {
            if (elements[i].textOffset > offset) builder.append(' ')
            offset = elements[i].textOffset + elements[i].text.length
            val element = elements[i]
            if (element is PsiInlineDocTag) {
                when (element.name) {
                    LITERAL_TAG -> generateLiteralValue(builder, element)
                    CODE_TAG, SYSTEM_PROPERTY_TAG -> generateCodeValue(element, builder)
                    LINK_TAG -> generateLinkValue(element, builder, false)
                    LINKPLAIN_TAG -> generateLinkValue(element, builder, true)
                }
            } else {
                appendPlainText(builder, element.text)
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
        builder.append("<code>")
        val pos = builder.length
        generateLiteralValue(builder, tag)
        builder.append("</code>")
        if (builder[pos] == '\n') builder.insert(
            pos,
            ' '
        ) // line break immediately after opening tag is ignored by JEditorPane
    }

    private fun generateLiteralValue(builder: StringBuilder, tag: PsiDocTag) {
        val literalValue = buildString {
            val children = tag.children
            for (i in 2 until children.size - 1) { // process all children except tag opening/closing elements
                val child = children[i]
                if (child is PsiDocToken && child.tokenType === JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS) continue
                var elementText = child.text
                if (child is PsiWhiteSpace) {
                    val pos = elementText.lastIndexOf('\n')
                    if (pos >= 0) elementText =
                        elementText.substring(0, pos + 1) // skip whitespace before leading asterisk
                }
                appendPlainText(this, StringUtil.escapeXmlEntities(elementText))
            }
        }
        builder.append(StringUtil.trimLeading(literalValue))
    }

    private fun generateLinkValue(tag: PsiInlineDocTag, builder: StringBuilder, plainLink: Boolean) {
        val tagElements = tag.dataElements
        val linkText: String = createLinkText(tagElements)
        if (linkText.isNotEmpty()) {
            val index = JavaDocUtil.extractReference(linkText)
            val referenceText = linkText.substring(0, index).trim { it <= ' ' }
            val label = StringUtil.nullize(linkText.substring(index).trim { it <= ' ' })
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
                if (tagElement.textOffset > offset) this.append(' ')
                offset = tagElement.textOffset + tagElement.text.length
                collectElementText(this, tagElement)
                if (i < tagElements.size - 1) {
                    this.append(' ')
                }
            }
        }.trim { it <= ' ' }
    }

    private fun generateLink(
        builder: StringBuilder,
        refText: String?,
        label: String?,
        context: PsiElement,
        plainLink: Boolean
    ) {
        var linkLabel = label
        if (label == null) {
            val manager = context.manager
            linkLabel = JavaDocUtil.getLabelText(manager.project, manager, refText, context)
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
            val referenceText = JavaDocUtil.getReferenceText(target.project, target)
            if (referenceText != null) {
                DocumentationManagerUtil.createHyperlink(builder, target, referenceText, linkLabel, plainLink)
            }
        }
    }
}