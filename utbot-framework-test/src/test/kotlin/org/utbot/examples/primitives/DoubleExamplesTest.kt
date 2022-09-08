package org.utbot.examples.primitives

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

@Suppress("SimplifyNegatedBinaryExpression")
internal class DoubleExamplesTest : UtValueTestCaseChecker(testClass = DoubleExamples::class) {
    @Test
    fun testCompareSum() {
        check(
            DoubleExamples::compareSum,
            eq(2),
            { a, b, r -> a + b > 5.6 && r == 1.0 },
            { a, b, r -> (a + b).isNaN() || a + b <= 5.6 && r == 0.0 }
        )
    }

    @Test
    fun testCompare() {
        check(
            DoubleExamples::compare,
            eq(2),
            { a, b, r -> a > b && r == 1.0 },
            { a, b, r -> !(a > b) && r == 0.0 }
        )
    }

    @Test
    fun testCompareWithDiv() {
        check(
            DoubleExamples::compareWithDiv,
            eq(2), // only two branches because division by zero is not an error with doubles
            { a, b, r -> a / (a + 0.5) > b && r == 1.0 },
            { a, b, r -> !(a / (a + 0.5) > b) && r == 0.0 }
        )
    }

    @Test
    fun testSimpleSum() {
        check(
            DoubleExamples::simpleSum,
            eq(4),
            { a, b, r -> (a + b).isNaN() && r == 0.0 },
            { a, b, r -> a + 1.1 + b > 10.1 && a + 1.1 + b < 11.125 && r == 1.1 },
            { a, b, r -> a + 1.1 + b <= 10.1 && r == 1.2 },
            { a, b, r -> a + 1.1 + b >= 11.125 && r == 1.2 }
        )
    }

    @Test
    fun testSum() {
        check(
            DoubleExamples::sum,
            eq(4),
            { a, b, r -> (a + b).isNaN() && r == 0.0 },
            { a, b, r -> a + 0.123124 + b > 11.123124 && a + b + 0.123124 < 11.125 && r == 1.1 },
            { a, b, r -> a + 0.123124 + b <= 11.123124 && r == 1.2 },
            { a, b, r -> a + 0.123124 + b >= 11.125 && r == 1.2 }
        )
    }

    @Test
    fun testSimpleMul() {
        check(
            DoubleExamples::simpleMul,
            eq(4),
            { a, b, r -> (a * b).isNaN() && r == 0.0 },
            { a, b, r -> a * b > 33.1 && a * b < 33.875 && r == 1.1 },
            { a, b, r -> a * b <= 33.1 && r == 1.2 },
            { a, b, r -> a * b >= 33.875 && r == 1.2 }
        )
    }

    @Test
    fun testMul() {
        check(
            DoubleExamples::mul,
            eq(6),
            { a, b, r -> (a * b).isNaN() && r == 0.0 }, // 0 * inf || a == nan || b == nan
            { a, b, r -> !(a * b > 33.32) && !(a * b > 33.333) && r == 1.3 }, // 1.3, 1-1 false, 2-1 false
            { a, b, r -> a * b == 33.333 && r == 1.3 }, // 1.3, 1-1 true, 1-2 false, 2-1 false
            { a, b, r -> a * b > 33.32 && a * b < 33.333 && r == 1.1 }, // 1.1, 1st true
            { a, b, r -> a * b > 33.333 && a * b < 33.7592 && r == 1.2 }, // 1.2, 1st false, 2nd true
            { a, b, r -> a * b >= 33.7592 && r == 1.3 } // 1.3, 1-1 false, 2-1 true, 2-2 false
        )
    }

    @Test
    fun testCheckNonInteger() {
        check(
            DoubleExamples::checkNonInteger,
            eq(3),
            { a, r -> !(a > 0.1) && r == 0.0 },
            { a, r -> a > 0.1 && !(a < 0.9) && r == 0.0 },
            { a, r -> a > 0.1 && a < 0.9 && r == 1.0 }
        )
    }

    @Test
    fun testDiv() {
        check(
            DoubleExamples::div,
            eq(1),
            { a, b, c, r -> r == (a + b) / c || (r!!.isNaN() && (a + b + c).isNaN()) }
        )
    }

    @Test
    fun testSimpleEquation() {
        check(
            DoubleExamples::simpleEquation,
            eq(2),
            { x, r -> x + x + x - 9 == x + 3 && r == 0 },
            { x, r -> x + x + x - 9 != x + 3 && r == 1 }
        )
    }

    @Test
    fun testSimpleNonLinearEquation() {
        check(
            DoubleExamples::simpleNonLinearEquation,
            eq(2),
            { x, r -> 3 * x - 9 == x + 3 && r == 0 },
            { x, r -> 3 * x - 9 != x + 3 && r == 1 }
        )
    }

    @Test
    fun testCheckNaN() {
        check(
            DoubleExamples::checkNaN,
            eq(4),
            { d, r -> !d.isNaN() && d < 0 && r == -1 },
            { d, r -> !d.isNaN() && d == 0.0 && r == 0 },
            { d, r -> !d.isNaN() && d > 0 && r == 1 },
            { d, r -> d.isNaN() && r == 100 }
        )
    }

    @Test
    fun testUnaryMinus() {
        check(
            DoubleExamples::unaryMinus,
            eq(2),
            { d, r -> !d.isNaN() && -d < 0 && r == -1 },
            { d, r -> d.isNaN() || -d >= 0 && r == 0 }
        )
    }

    @Test
    fun testDoubleInfinity() {
        check(
            DoubleExamples::doubleInfinity,
            eq(3),
            { d, r -> d == Double.POSITIVE_INFINITY && r == 1 },
            { d, r -> d == Double.NEGATIVE_INFINITY && r == 2 },
            { d, r -> !d.isInfinite() && r == 3 },
        )
    }
}
