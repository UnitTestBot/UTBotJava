package org.utbot.summary.comment.customtags.fuzzer

/**
 * Represents a set of plugin's custom JavaDoc tags.
 */
data class CommentWithCustomTagForTestProducedByFuzzer(
    val classUnderTest: String = "",
    val methodUnderTest: String = "",
    val expectedResult: String = "",
    val actualResult: String = "",
    var returnsFrom: String = "",
    var throwsException: String = ""
)