package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

internal class AliasExamplesTest : UtValueTestCaseChecker(
    testClass = AliasExamplesTest::class
) {
    @Test
    fun testBad550() {
        check(
            AliasExamples::bad550,
            eq(-1),
        )
    }

    @Test
    fun testBad551() {
        check(
            AliasExamples::bad551,
            eq(-1),
        )
    }

    @Test
    fun testBad552() {
        check(
            AliasExamples::bad552,
            eq(-1),
        )
    }

    @Test
    fun testBad553() {
        check(
            AliasExamples::bad553,
            eq(-1),
        )
    }

    @Test
    fun testBad554() {
        check(
            AliasExamples::bad554,
            eq(-1),
        )
    }

}