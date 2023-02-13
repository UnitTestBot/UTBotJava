package org.utbot.summary.comment.customtags.symbolic

import org.utbot.framework.plugin.api.DocRegularLineStmt
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.summary.comment.customtags.fuzzer.CommentWithCustomTagForTestProducedByFuzzer

/**
 * Provides a list of supported custom JavaDoc tags.
 */
class CustomJavaDocTagProvider {
    // The tags' order is important because plugin builds final JavaDoc comment according to it.
    fun getPluginCustomTags(): List<CustomJavaDocTag> =
        listOf(
            CustomJavaDocTag.ClassUnderTest,
            CustomJavaDocTag.MethodUnderTest,
            CustomJavaDocTag.ExpectedResult,
            CustomJavaDocTag.ActualResult,
            CustomJavaDocTag.Executes,
            CustomJavaDocTag.Invokes,
            CustomJavaDocTag.Iterates,
            CustomJavaDocTag.SwitchCase,
            CustomJavaDocTag.Recursion,
            CustomJavaDocTag.ReturnsFrom,
            CustomJavaDocTag.CaughtException,
            CustomJavaDocTag.ThrowsException,
            CustomJavaDocTag.DetectsSuspiciousBehavior,
        )
}

sealed class CustomJavaDocTag(
    val name: String,
    val message: String,
    private val valueRetriever: (CustomJavaDocComment) -> Any,
    private val valueRetrieverFuzzer: ((CommentWithCustomTagForTestProducedByFuzzer) -> Any)? // TODO: remove after refactoring
) {
    object ClassUnderTest :
        CustomJavaDocTag(
            "utbot.classUnderTest",
            "Class under test",
            CustomJavaDocComment::classUnderTest,
            CommentWithCustomTagForTestProducedByFuzzer::classUnderTest
        )

    object MethodUnderTest :
        CustomJavaDocTag(
            "utbot.methodUnderTest",
            "Method under test",
            CustomJavaDocComment::methodUnderTest,
            CommentWithCustomTagForTestProducedByFuzzer::methodUnderTest
        )

    object ExpectedResult :
        CustomJavaDocTag("utbot.expectedResult", "Expected result", CustomJavaDocComment::expectedResult, null)

    object ActualResult :
        CustomJavaDocTag("utbot.actualResult", "Actual result", CustomJavaDocComment::actualResult, null)

    object Executes :
        CustomJavaDocTag("utbot.executesCondition", "Executes condition", CustomJavaDocComment::executesCondition, null)

    object Invokes : CustomJavaDocTag("utbot.invokes", "Invokes", CustomJavaDocComment::invokes, null)
    object Iterates : CustomJavaDocTag("utbot.iterates", "Iterates", CustomJavaDocComment::iterates, null)
    object SwitchCase :
        CustomJavaDocTag("utbot.activatesSwitch", "Activates switch", CustomJavaDocComment::switchCase, null)

    object Recursion :
        CustomJavaDocTag("utbot.triggersRecursion", "Triggers recursion ", CustomJavaDocComment::recursion, null)

    object ReturnsFrom : CustomJavaDocTag("utbot.returnsFrom", "Returns from", CustomJavaDocComment::returnsFrom, null)
    object CaughtException :
        CustomJavaDocTag("utbot.caughtException", "Caught exception", CustomJavaDocComment::caughtException, null)

    object ThrowsException :
        CustomJavaDocTag("utbot.throwsException", "Throws exception", CustomJavaDocComment::throwsException, null)

    object DetectsSuspiciousBehavior :
        CustomJavaDocTag("utbot.detectsSuspiciousBehavior", "Detects suspicious behavior", CustomJavaDocComment::detectsSuspiciousBehavior, null)

    fun generateDocStatement(comment: CustomJavaDocComment): DocRegularStmt? =
        when (val value = valueRetriever.invoke(comment)) {
            is String -> value.takeIf { it.isNotEmpty() }?.let {
                DocRegularStmt("@$name $value\n")
            }
            is List<*> -> value.takeIf { it.isNotEmpty() }?.let {
                val valueToString = value.joinToString(separator = "\n", postfix = "\n") { "@$name $it" }

                DocRegularStmt(valueToString)
            }
            else -> null
        }

    // TODO: could be universal with the function above after creation of hierarchy data classes related to the comments
    fun generateDocStatementForTestProducedByFuzzer(comment: CommentWithCustomTagForTestProducedByFuzzer): DocStatement? {
        if (valueRetrieverFuzzer != null) { //TODO: it required only when we have two different retrievers
            return when (val value = valueRetrieverFuzzer!!.invoke(comment)) { // TODO: unsafe !! - resolve
                is String -> value.takeIf { it.isNotEmpty() }?.let {
                    DocRegularLineStmt("@$name $value")
                }

                is List<*> -> value.takeIf { it.isNotEmpty() }?.let {
                    val valueToString = value.joinToString(separator = ",\n", postfix = "\n")

                    DocRegularStmt("@$name $valueToString")
                }

                else -> null
            }
        }
        return null
    }
}