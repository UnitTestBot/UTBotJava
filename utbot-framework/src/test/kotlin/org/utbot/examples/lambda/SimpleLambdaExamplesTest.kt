package org.utbot.examples.lambda

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.isException

class SimpleLambdaExamplesTest : AbstractTestCaseGeneratorTest(testClass = SimpleLambdaExamples::class) {
    @Test
    fun testBiFunctionLambdaExample() {
        checkWithException(
            SimpleLambdaExamples::biFunctionLambdaExample,
            eq(2),
            { a, b, r -> b == 0 && r.isException<ArithmeticException>() },
            { a, b, r -> b != 0 && r.getOrThrow() == a / b },
        )
    }

    @Test
    @Disabled("TODO 0 executions https://github.com/UnitTestBot/UTBotJava/issues/192")
    fun testChoosePredicate() {
        check(
            SimpleLambdaExamples::choosePredicate,
            eq(2),
            { b, r -> b && !r!!.test(null) && r.test(0) },
            { b, r -> !b && r!!.test(null) && !r.test(0) },
            // TODO coverage calculation fails https://github.com/UnitTestBot/UTBotJava/issues/192
        )
    }
}