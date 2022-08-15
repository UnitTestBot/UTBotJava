package org.utbot.summary.comment

import org.utbot.framework.plugin.api.DocRegularStmt

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
            CustomJavaDocTag.ReturnsFrom,
            CustomJavaDocTag.ThrowsException,
        )
}

sealed class CustomJavaDocTag(
    val name: String,
    val message: String,
    private val valueRetriever: (CustomJavaDocComment) -> Any
) {
    object ClassUnderTest :
        CustomJavaDocTag("utbot.classUnderTest", "Class under test", CustomJavaDocComment::classUnderTest)

    object MethodUnderTest :
        CustomJavaDocTag("utbot.methodUnderTest", "Method under test", CustomJavaDocComment::methodUnderTest)

    object ExpectedResult :
        CustomJavaDocTag("utbot.expectedResult", "Expected result", CustomJavaDocComment::expectedResult)

    object ActualResult : CustomJavaDocTag("utbot.actualResult", "Actual result", CustomJavaDocComment::actualResult)
    object Executes :
        CustomJavaDocTag("utbot.executesCondition", "Executes condition", CustomJavaDocComment::executesCondition)

    object Invokes : CustomJavaDocTag("utbot.invokes", "Invokes", CustomJavaDocComment::invokes)
    object Iterates : CustomJavaDocTag("utbot.iterates", "Iterates", CustomJavaDocComment::iterates)
    object ReturnsFrom : CustomJavaDocTag("utbot.returnsFrom", "Returns from", CustomJavaDocComment::returnsFrom)
    object ThrowsException :
        CustomJavaDocTag("utbot.throwsException", "Throws exception", CustomJavaDocComment::throwsException)

    fun generateDocStatement(comment: CustomJavaDocComment): DocRegularStmt? =
        when (val value = valueRetriever.invoke(comment)) {
            is String -> value.takeIf { it.isNotEmpty() }?.let {
                DocRegularStmt("@$name $value\n")
            }
            is List<*> -> value.takeIf { it.isNotEmpty() }?.let {
                val valueToString = value.joinToString(separator = ",\n", postfix = "\n")

                DocRegularStmt("@$name $valueToString")
            }
            else -> null
        }
}