package org.utbot.summary.comment.customtags.fuzzer

import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.comment.customtags.symbolic.CustomJavaDocTagProvider

/**
 * Builds JavaDoc comments for generated tests using plugin's custom JavaDoc tags.
 */
class CommentWithCustomTagForTestProducedByFuzzerBuilder(
    val methodDescription: FuzzedMethodDescription,
    val values: List<FuzzedValue>,
    val result: UtExecutionResult?
) {

    /**
     * Collects statements for final JavaDoc comment.
     */
    fun buildDocStatements(): List<DocStatement> {
        val comment = buildCustomJavaDocComment()
        val docStatementList =
            CustomJavaDocTagProvider().getPluginCustomTags().mapNotNull { it.generateDocStatementForTestProducedByFuzzer(comment) }
        return listOf(DocCustomTagStatement(docStatementList))
    }

    private fun buildCustomJavaDocComment(): CommentWithCustomTagForTestProducedByFuzzer {
        /*val methodReference = getMethodReference(
            currentMethod.declaringClass.name,
            currentMethod.name,
            currentMethod.parameterTypes
        )
        val classReference = getClassReference(currentMethod.declaringClass.javaStyleName)*/

        val javaDocComment = CommentWithCustomTagForTestProducedByFuzzer(
            classUnderTest = methodDescription.className!!,
            methodUnderTest = methodDescription.name,
        )


        return javaDocComment
    }
}