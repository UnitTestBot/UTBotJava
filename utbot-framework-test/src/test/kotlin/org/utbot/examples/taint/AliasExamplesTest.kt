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
    fun testParamDependentGood() {
        check(
            AliasExamples::paramDependentGood,
            eq(-1),
        )
    }

    @Test
    fun testPassSecondParamBad() {
        check(
            AliasExamples::passSecondParamBad,
            eq(-1),
        )
    }

    @Test
    fun testPassSecondParamGood() {
        check(
            AliasExamples::passSecondParamGood,
            eq(-1),
        )
    }

    @Test
    fun testPassFirstParamGood() {
        check(
            AliasExamples::passFirstParamGood,
            eq(-1),
        )
    }

    @Test
    fun testPassFirstParamBad() {
        check(
            AliasExamples::passFirstParamBad,
            eq(-1),
        )
    }


    @Test
    fun testParamDependentBad() {
        check(
            AliasExamples::paramDependentBad,
            eq(-1),
        )
    }

    @Test
    fun testClearSecondParameter() {
        check(
            AliasExamples::clearSecondParameter,
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