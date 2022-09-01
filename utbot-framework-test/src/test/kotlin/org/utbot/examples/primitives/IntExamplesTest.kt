package org.utbot.examples.primitives

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.testcheckers.eq

@Suppress("ConvertTwoComparisonsToRangeCheck")
internal class IntExamplesTest : UtValueTestCaseChecker(testClass = IntExamples::class) {
    @Test
    @Disabled("SAT-1009 [JAVA] Engine can't analyze isInteger")
    fun testIsInteger() {
        val method = IntExamples::isInteger
        checkStaticMethod(
            method,
            eq(2),
            { value, r -> runCatching { Integer.valueOf(value) }.isSuccess && r == true },
            { value, r -> runCatching { Integer.valueOf(value) }.isFailure && r == false },
        )
    }

    @Test
    fun testMax() {
        check(
            IntExamples::max,
            eq(2),
            { x, y, r -> x > y && r == x },
            { x, y, r -> x <= y && r == y }
        )
    }

    @Test
    fun testPreferableLt() {
        check(
            IntExamples::preferableLt,
            eq(2),
            { x, r -> x == 41 && r == 41 },
            { x, r -> x == 42 && r == 42 }
        )
    }

    @Test
    fun testPreferableLe() {
        check(
            IntExamples::preferableLe,
            eq(2),
            { x, r -> x == 42 && r == 42 },
            { x, r -> x == 43 && r == 43 }
        )
    }

    @Test
    fun testPreferableGe() {
        check(
            IntExamples::preferableGe,
            eq(2),
            { x, r -> x == 42 && r == 42 },
            { x, r -> x == 41 && r == 41 }
        )
    }

    @Test
    fun testPreferableGt() {
        check(
            IntExamples::preferableGt,
            eq(2),
            { x, r -> x == 43 && r == 43 },
            { x, r -> x == 42 && r == 42 }
        )
    }


    @Test
    fun testCompare() {
        check(
            IntExamples::complexCompare,
            eq(6),
            { a, b, r -> a < b && b < 11 && r == 0 },
            { a, b, r -> a < b && b > 11 && r == 1 },
            { a, b, r -> a == b && b == 11 && r == 3 },
            { a, b, r -> a == b && b != 11 && r == 6 },
            { a, b, r -> a < b && b == 11 && r == 6 },
            { a, b, r -> a > b && r == 6 }
        )
    }

    @Test
    fun testComplexCondition() {
        check(
            IntExamples::complexCondition,
            eq(3),
            { _, b, r -> b + 10 >= b + 22 && r == 0 }, // negative overflow, result = 1
            { a, b, r -> b + 10 < b + 22 && b + 22 >= a + b + 10 && r == 0 },
            { a, b, r -> b + 10 < b + 22 && b + 22 < a + b + 10 && r == 1 } // overflow involved
        )
    }

    @Test
    fun testOrderCheck() {
        check(
            IntExamples::orderCheck,
            eq(3),
            { first, second, _, r -> first >= second && r == false },
            { first, second, third, r -> first < second && second >= third && r == false },
            { first, second, third, r -> first < second && second < third && r == true }
        )
    }

    @Test
    fun testOrderCheckWithMethods() {
        check(
            IntExamples::orderCheckWithMethods,
            eq(3),
            { first, second, _, r -> first >= second && r == false },
            { first, second, third, r -> first < second && second >= third && r == false },
            { first, second, third, r -> first < second && second < third && r == true }
        )
    }
}