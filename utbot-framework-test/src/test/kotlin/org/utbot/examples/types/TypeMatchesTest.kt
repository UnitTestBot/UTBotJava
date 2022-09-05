package org.utbot.examples.types

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

@Suppress("SimplifyNegatedBinaryExpression")
internal class TypeMatchesTest : UtValueTestCaseChecker(testClass = TypeMatches::class) {
    @Test
    fun testCompareDoubleByte() {
        check(
            TypeMatches::compareDoubleByte,
            eq(2),
            { a, b, r -> a < b && r == 0.0 },
            { a, b, r -> !(a < b) && r == 1.0 }
        )
    }

    @Test
    fun testCompareShortLong() {
        check(
            TypeMatches::compareShortLong,
            eq(2),
            { a, b, r -> a < b && r == 0.toShort() },
            { a, b, r -> a >= b && r == 1.toShort() }
        )
    }

    @Test
    fun testCompareFloatDouble() {
        check(
            TypeMatches::compareFloatDouble,
            eq(2),
            { a, b, r -> a < b && r == 0.0f },
            { a, b, r -> !(a < b) && r == 1.0f }
        )
    }

    @Test
    fun testSumByteAndShort() {
        check(
            TypeMatches::sumByteAndShort,
            eq(3),
            { a, b, r -> a + b > Short.MAX_VALUE && r == 1 },
            { a, b, r -> a + b < Short.MIN_VALUE && r == 2 },
            { a, b, r -> a + b in Short.MIN_VALUE..Short.MAX_VALUE && r == 3 },
        )
    }

    @Test
    fun testSumShortAndChar() {
        check(
            TypeMatches::sumShortAndChar,
            eq(3),
            { a, b, r -> a + b.toInt() > Char.MAX_VALUE.toInt() && r == 1 },
            { a, b, r -> a + b.toInt() < Char.MIN_VALUE.toInt() && r == 2 },
            { a, b, r -> a + b.toInt() in Char.MIN_VALUE.toInt()..Char.MAX_VALUE.toInt() && r == 3 },
        )
    }
}
