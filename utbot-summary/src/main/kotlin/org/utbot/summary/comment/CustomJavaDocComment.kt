package org.utbot.summary.comment

/**
 * Represents a set of plugin's custom JavaDoc tags.
 */
data class CustomJavaDocComment(
    val classUnderTest: String,
    val methodUnderTest: String,
    val expectedResult: String?,
    val actualResult: String?,
    var executesCondition: String?,
    var invokes: String?,
    var iterates: String?,
    var returnsFrom: String?,
    var throwsException: String?
)