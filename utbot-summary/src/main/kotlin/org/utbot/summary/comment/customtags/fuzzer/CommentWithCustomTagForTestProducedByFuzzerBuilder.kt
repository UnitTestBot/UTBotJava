package org.utbot.summary.comment.customtags.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.SummarySentenceConstants
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
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
        val methodReference = getMethodReference(
            methodDescription.packageName!! + "." + methodDescription.className!!,
            methodDescription.compilableName!!,
            methodDescription.parameters
        )
        val classReference = getClassReference(methodDescription.packageName!! + "." +methodDescription.className!!)

        val javaDocComment = CommentWithCustomTagForTestProducedByFuzzer(
            classUnderTest = classReference.replace(CARRIAGE_RETURN, ""),
            methodUnderTest = methodReference.replace(CARRIAGE_RETURN, ""),
        )

        return javaDocComment
    }

    /**
     * Returns a reference to the invoked method.
     *
     * It looks like {@link packageName.className#methodName(type1, type2)}.
     *
     * In case when an enclosing class in nested, we need to replace '$' with '.'
     * to render the reference.
     */
    private fun getMethodReference(className: String, methodName: String, methodParameterTypes: List<ClassId>): String {
        val prettyClassName: String = className.replace("$", ".")

        return if (methodParameterTypes.isEmpty()) {
            "{@link $prettyClassName#$methodName()}"
        } else {
            val methodParametersAsString = methodParameterTypes.joinToString(",") { it.canonicalName }
            "{@link $prettyClassName#$methodName($methodParametersAsString)}"
        }
    }

    /**
     * Returns a reference to the class.
     * Replaces '$' with '.' in case a class is nested.
     */
    private fun getClassReference(fullClassName: String): String {
        return "{@link ${fullClassName.replace("$", ".")}}"
    }
}