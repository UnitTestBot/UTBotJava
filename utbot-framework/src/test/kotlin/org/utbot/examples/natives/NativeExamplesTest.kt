package org.utbot.examples.natives

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.ge
import org.junit.jupiter.api.Test

internal class NativeExamplesTest : AbstractTestCaseGeneratorTest(testClass = NativeExamples::class) {

    @Test
    fun testFindAndPrintSum() {
        check(
            NativeExamples::findAndPrintSum,
            ge(1),
            coverage = DoNotCalculate,
        )
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