package org.utbot.summary.comment.customtags.fuzzer

import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.comment.customtags.getClassReference
import org.utbot.summary.comment.customtags.getMethodReferenceForFuzzingTest
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
            CustomJavaDocTagProvider().getPluginCustomTags()
                .mapNotNull { it.generateDocStatementForTestProducedByFuzzer(comment) }
        return listOf(DocCustomTagStatement(docStatementList))
    }

    private fun buildCustomJavaDocComment(): CommentWithCustomTagForTestProducedByFuzzer {
        val packageName = methodDescription.packageName
        val className = methodDescription.className
        val methodName = methodDescription.compilableName

        return if (packageName != null && className != null && methodName != null) {
            val fullClassName = "$packageName.$className"

            val methodReference = getMethodReferenceForFuzzingTest(
                fullClassName,
                methodName,
                methodDescription.parameters,
                false
            ).replace(CARRIAGE_RETURN, "")

            val classReference = getClassReference(fullClassName).replace(CARRIAGE_RETURN, "")

            CommentWithCustomTagForTestProducedByFuzzer(
                classUnderTest = classReference,
                methodUnderTest = methodReference,
            )
        } else {
            CommentWithCustomTagForTestProducedByFuzzer()
        }
    }
}
