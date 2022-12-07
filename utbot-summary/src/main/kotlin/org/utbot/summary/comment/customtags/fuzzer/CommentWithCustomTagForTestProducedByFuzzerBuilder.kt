package org.utbot.summary.comment.customtags.fuzzer

import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.comment.customtags.getClassReference
import org.utbot.summary.comment.customtags.getFullClassName
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
        val canonicalName = methodDescription.canonicalName
        val isNested = methodDescription.isNested

        return if (packageName != null && className != null && methodName != null) {
            val fullClassName = getFullClassName(canonicalName, packageName, className, isNested)

            val methodReference = getMethodReferenceForFuzzingTest(
                fullClassName,
                methodName,
                methodDescription.parameters,
                false
            )

            val classReference = getClassReference(fullClassName)

            CommentWithCustomTagForTestProducedByFuzzer(
                classUnderTest = classReference,
                methodUnderTest = methodReference,
            )
        } else {
            CommentWithCustomTagForTestProducedByFuzzer()
        }
    }
}
