package org.utbot.examples.lambda

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException

// TODO failed Kotlin compilation (generics) SAT-1332
class SimpleLambdaExamplesTest : UtValueTestCaseChecker(
    testClass = SimpleLambdaExamples::class,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testBiFunctionLambdaExample() {
        checkWithException(
            SimpleLambdaExamples::biFunctionLambdaExample,
            eq(2),
            { _, b, r -> b == 0 && r.isException<ArithmeticException>() },
            { a, b, r -> b != 0 && r.getOrThrow() == a / b },
        )
    }

    @Test
    fun testChoosePredicate() {
        check(
            SimpleLambdaExamples::choosePredicate,
            eq(2),
            { b, r -> b && !r!!.test(null) && r.test(0) },
            { b, r -> !b && r!!.test(null) && !r.test(0) },
            coverage = DoNotCalculate // coverage could not be calculated since method result is lambda
        )
    }
}
