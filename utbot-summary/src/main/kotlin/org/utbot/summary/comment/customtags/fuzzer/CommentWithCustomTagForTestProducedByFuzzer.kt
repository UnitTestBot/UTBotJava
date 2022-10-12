package org.utbot.summary.comment.customtags.fuzzer

import org.utbot.summary.comment.EMPTY_STRING

/**
 * Represents a set of plugin's custom JavaDoc tags.
 */
data class CommentWithCustomTagForTestProducedByFuzzer(
    val classUnderTest: String = EMPTY_STRING,
    val methodUnderTest: String = EMPTY_STRING,
    val expectedResult: String = EMPTY_STRING,
    val actualResult: String = EMPTY_STRING,
    var returnsFrom: String = EMPTY_STRING,
    var throwsException: String = EMPTY_STRING
)