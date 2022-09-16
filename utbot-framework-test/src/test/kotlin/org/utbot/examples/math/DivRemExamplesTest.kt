package org.utbot.examples.math

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class DivRemExamplesTest : UtValueTestCaseChecker(testClass = DivRemExamples::class) {
    @Test
    fun testDiv() {
        checkWithException(
            DivRemExamples::div,
            eq(2),
            { _, y, r -> y == 0 && r.isException<ArithmeticException>() },
            { x, y, r -> y != 0 && r.getOrNull() == x / y }
        )
    }

    @Test
    fun testRem() {
        checkWithException(
            DivRemExamples::rem,
            eq(2),
            { _, y, r -> y == 0 && r.isException<ArithmeticException>() },
            { x, y, r -> y != 0 && r.getOrNull() == x % y }
        )
    }

    @Test
    fun testRemPositiveConditional() {
        checkWithException(
            DivRemExamples::remPositiveConditional,
            eq(3),
            { d, r -> d == 0 && r.isException<ArithmeticException>() },
            { d, r -> d != 0 && 11 % d == 2 && r.getOrNull() == true },
            { d, r -> d != 0 && 11 % d != 2 && r.getOrNull() == false }
        )
    }

    @Test
    fun testRemNegativeConditional() {
        checkWithException(
            DivRemExamples::remNegativeConditional,
            eq(3),
            { d, r -> d == 0 && r.isException<ArithmeticException>() },
            { d, r -> d != 0 && -11 % d == -2 && r.getOrNull() == true },
            { d, r -> d != 0 && -11 % d != -2 && r.getOrNull() == false }
        )
    }

    @Test
    fun testRemWithConditions() {
        checkWithException(
            DivRemExamples::remWithConditions,
            eq(4),
            { d, r -> d < 0 && r.getOrNull() == false },
            { d, r -> d == 0 && r.isException<ArithmeticException>() },
            { d, r -> d > 0 && -11 % d == -2 && r.getOrNull() == true },
            { d, r -> d > 0 && -11 % d != -2 && r.getOrNull() == false }
        )
    }

    @Test
    fun testRemDoubles() {
        check(
            DivRemExamples::remDoubles,
            eq(1)
        )
    }

    @Test
    fun testRemDoubleInt() {
        check(
            DivRemExamples::remDoubleInt,
            eq(1)
        )
    }
}