package org.utbot.examples.natives

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.ge
import org.junit.jupiter.api.Test
import org.utbot.examples.withSolverTimeoutInMillis

internal class NativeExamplesTest : UtValueTestCaseChecker(testClass = NativeExamples::class) {

    @Test
    fun testFindAndPrintSum() {
        // TODO related to the https://github.com/UnitTestBot/UTBotJava/issues/131
        withSolverTimeoutInMillis(5000) {
            check(
                NativeExamples::findAndPrintSum,
                ge(1),
                coverage = DoNotCalculate,
            )
        }
    }

    @Test
    fun testFindSumWithMathRandom() {
        check(
            NativeExamples::findSumWithMathRandom,
            eq(1),
            coverage = DoNotCalculate,
        )
    }
}