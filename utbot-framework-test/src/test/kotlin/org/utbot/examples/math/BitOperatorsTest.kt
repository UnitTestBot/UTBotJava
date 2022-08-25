package org.utbot.examples.math

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class BitOperatorsTest : UtValueTestCaseChecker(testClass = BitOperators::class) {
    @Test
    fun testComplement() {
        check(
            BitOperators::complement,
            eq(2),
            { x, r -> x == -2 && r == true },
            { x, r -> x != -2 && r == false }
        )
    }

    @Test
    fun testXor() {
        check(
            BitOperators::xor,
            eq(2),
            { x, y, r -> x == y && r == true },
            { x, y, r -> x != y && r == false }
        )
    }

    @Test
    fun testOr() {
        check(
            BitOperators::or,
            eq(2),
            { x, r -> x < 16 && (x and 0xfffffff8.toInt()) == 8 && r == true },
            { x, r -> x >= 16 || (x and 0xfffffff8.toInt()) != 8 && r == false }
        )
    }

    @Test
    @kotlin.ExperimentalStdlibApi
    fun testAnd() {
        check(
            BitOperators::and,
            eq(2),
            { x, r -> x.countOneBits() <= 1 && r == true },
            { x, r -> x.countOneBits() > 1 && r == false }
        )
    }

    @Test
    fun testBooleanNot() {
        check(
            BitOperators::booleanNot,
            eq(3),
            { a, b, r -> a && b && r == 100 },
            { a, b, r -> a && !b && r == 200 },
            { a, b, r -> !a && !b && r == 200 },
            coverage = atLeast(91)
        )
    }

    @Test
    fun testBooleanXor() {
        check(
            BitOperators::booleanXor,
            eq(1)
        )
    }

    @Test
    fun testBooleanOr() {
        check(
            BitOperators::booleanOr,
            eq(1)
        )
    }

    @Test
    fun testBooleanAnd() {
        check(
            BitOperators::booleanAnd,
            eq(1)
        )
    }

    @Test
    fun testBooleanXorCompare() {
        check(
            BitOperators::booleanXorCompare,
            eq(2),
            { a, b, r -> a != b && r == 1 },
            { a, b, r -> a == b && r == 0 }
        )
    }

    @Test
    fun testShl() {
        check(
            BitOperators::shl,
            eq(2),
            { x, r -> x == 1 && r == true },
            { x, r -> x != 1 && r == false }
        )
    }

    @Test
    fun testShlLong() {
        check(
            BitOperators::shlLong,
            eq(2),
            { x, r -> x == 1L && r == true },
            { x, r -> x != 1L && r == false }
        )
    }

    @Test
    fun testShlWithBigLongShift() {
        check(
            BitOperators::shlWithBigLongShift,
            eq(3),
            { shift, r -> shift < 40 && r == 1 },
            { shift, r -> shift >= 40 && shift and 0b11111 == 4L && r == 2 },
            { shift, r -> shift >= 40 && shift and 0b11111 != 4L && r == 3 },
        )
    }

    @Test
    fun testShr() {
        check(
            BitOperators::shr,
            eq(2),
            { x, r -> x shr 1 == 1 && r == true },
            { x, r -> x shr 1 != 1 && r == false }
        )
    }

    @Test
    fun testShrLong() {
        check(
            BitOperators::shrLong,
            eq(2),
            { x, r -> x shr 1 == 1L && r == true },
            { x, r -> x shr 1 != 1L && r == false }
        )
    }

    @Test
    fun testUshr() {
        check(
            BitOperators::ushr,
            eq(2),
            { x, r -> x ushr 1 == 1 && r == true },
            { x, r -> x ushr 1 != 1 && r == false }
        )
    }

    @Test
    fun testUshrLong() {
        check(
            BitOperators::ushrLong,
            eq(2),
            { x, r -> x ushr 1 == 1L && r == true },
            { x, r -> x ushr 1 != 1L && r == false }
        )
    }

    @Test
    fun testSign() {
        check(
            BitOperators::sign,
            eq(3),
            { x, r -> x > 0 && r == 1 },
            { x, r -> x == 0 && r == 0 },
            { x, r -> x < 0 && r == -1 }
        )
    }
}