package org.utbot.summary.comment

/**
 * Represents a set of plugin's custom JavaDoc tags.
 */
data class CustomJavaDocComment(
    val classUnderTest: String = "",
    val methodUnderTest: String = "",
    val expectedResult: String = "",
    val actualResult: String = "",
    var executesCondition: List<String> = listOf(),
    var invokes: List<String> = listOf(),
    var iterates: List<String> = listOf(),
    var switchCase: String = "",
    var recursion: String = "",
    var returnsFrom: String = "",
    var countedReturn: String = "",
    var caughtException: String = "",
    var throwsException: String = ""
)