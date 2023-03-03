package org.utbot.summary.comment.customtags.symbolic

import org.utbot.summary.comment.classic.symbolic.EMPTY_STRING

/**
 * Represents a set of plugin's custom JavaDoc tags.
 */
data class CustomJavaDocComment(
    val classUnderTest: String = EMPTY_STRING,
    val methodUnderTest: String = EMPTY_STRING,
    val expectedResult: String = EMPTY_STRING,
    val actualResult: String = EMPTY_STRING,
    var executesCondition: List<String> = listOf(),
    var invokes: List<String> = listOf(),
    var iterates: List<String> = listOf(),
    var switchCase: String = EMPTY_STRING,
    var recursion: String = EMPTY_STRING,
    var returnsFrom: String = EMPTY_STRING,
    var countedReturn: String = EMPTY_STRING,
    var caughtException: String = EMPTY_STRING,
    var throwsException: String = EMPTY_STRING,
    var detectsSuspiciousBehavior: String = EMPTY_STRING
)