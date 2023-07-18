package org.utbot.examples.natives

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withSolverTimeoutInMillis
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

// TODO Kotlin mocks generics https://github.com/UnitTestBot/UTBotJava/issues/88
internal class NativeExamplesTest : UtValueTestCaseChecker(
    testClass = NativeExamples::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {

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