package org.utbot.examples.types

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

@Suppress("SimplifyNegatedBinaryExpression")
internal class CastExamplesTest : UtValueTestCaseChecker(testClass = CastExamples::class) {
    @Test
    fun testLongToByte() {
        check(
            CastExamples::longToByte,
            eq(3),
            { a, b, r -> (a.toByte() + b.toByte()).toByte() > 10 && r == (a.toByte() + b.toByte()).toByte() },
            { a, b, r -> (a.toByte() + b.toByte()).toByte() <= 10 && a.toByte() > b.toByte() && r == (-1).toByte() },
            { a, b, r -> (a.toByte() + b.toByte()).toByte() <= 10 && a.toByte() <= b.toByte() && r == (0).toByte() },
        )
    }

    @Test
    fun testShortToLong() {
        check(
            CastExamples::shortToLong,
            eq(3),
            { a, b, r -> a + b > 10 && r == a.toLong() + b.toLong() },
            { a, b, r -> a + b <= 10 && a > b && r == -1L },
            { a, b, r -> a + b <= 10 && a <= b && r == 0L },
        )
    }

    @Test
    fun testFloatToDouble() {
        check(
            CastExamples::floatToDouble,
            eq(4),
            { a, b, r -> a.toDouble() + b.toDouble() > Float.MAX_VALUE && r == 2.0 },
            { a, b, r -> a.toDouble() + b.toDouble() > 10 && r == 1.0 },
            { a, b, r -> !(a.toDouble() + b.toDouble() > 10) && !(a.toDouble() > b.toDouble()) && r == 0.0 },
            { a, b, r -> !(a.toDouble() + b.toDouble() > 10) && a.toDouble() > b.toDouble() && r == -1.0 },
        )
    }

    @Test
    fun testDoubleToFloatArray() {
        check(
            CastExamples::doubleToFloatArray,
            eq(2),
            { x, r -> x.toFloat() + 5 > 20 && r == 1.0f },
            { x, r -> !(x.toFloat() + 5 > 20) && r == 0.0f }
        )
    }

    @Test
    fun testFloatToInt() {
        check(
            CastExamples::floatToInt,
            eq(3),
            { x, r -> x < 0 && x.toInt() < 0 && r == 1 },
            { x, r -> x < 0 && x.toInt() >= 0 && r == 2 },
            { x, r -> !(x < 0) && r == 3 },
        )
    }

    @Test
    fun testShortToChar() {
        check(
            CastExamples::shortToChar,
            eq(3),
            { a, b, r -> (a.charInt() + b.charInt()).charInt() > 10 && r == (a.charInt() + b.charInt()).toChar() },
            { a, b, r -> (a.charInt() + b.charInt()).charInt() <= 10 && a.charInt() <= b.charInt() && r == (0).toChar() },
            { a, b, r -> (a.charInt() + b.charInt()).charInt() <= 10 && a.charInt() > b.charInt() && r == (-1).toChar() },
        )
    }

    // Special cast to emulate Java binary numeric promotion
    private fun Number.charInt() = toChar().toInt()
}