package org.utbot.examples.types

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class TypeBordersTest : UtValueTestCaseChecker(testClass = TypeBorders::class) {
    @Test
    fun testByteBorder() {
        check(
            TypeBorders::byteBorder,
            eq(3),
            { x, r -> x == Byte.MIN_VALUE && r == 3 },
            { x, r -> x == Byte.MAX_VALUE && r == 2 },
            { x, r -> x > Byte.MIN_VALUE && x < Byte.MAX_VALUE && r == 4 },
            coverage = atLeast(75)
        )
    }

    @Test
    fun testShortBorder() {
        check(
            TypeBorders::shortBorder,
            eq(3),
            { x, r -> x == Short.MIN_VALUE && r == 3 },
            { x, r -> x == Short.MAX_VALUE && r == 2 },
            { x, r -> x > Short.MIN_VALUE && x < Short.MAX_VALUE && r == 4 },
            coverage = atLeast(75)
        )
    }

    @Test
    fun testCharBorder() {
        check(
            TypeBorders::charBorder,
            eq(3),
            { x, r -> x == Char.MIN_VALUE && r == 3 },
            { x, r -> x == Char.MAX_VALUE && r == 2 },
            { x, r -> x > Char.MIN_VALUE && x < Char.MAX_VALUE && r == 4 },
            coverage = atLeast(75)
        )
    }

    @Test
    fun testIntBorder() {
        check(
            TypeBorders::intBorder,
            eq(3),
            { x, r -> x == Int.MIN_VALUE && r == 3 },
            { x, r -> x == Int.MAX_VALUE && r == 2 },
            { x, r -> x > Int.MIN_VALUE && x < Int.MAX_VALUE && r == 4 },
            coverage = atLeast(75)
        )
    }

    @Test
    fun testLongBorder() {
        check(
            TypeBorders::longBorder,
            eq(3),
            { x, r -> x == Long.MIN_VALUE && r == 3 },
            { x, r -> x == Long.MAX_VALUE && r == 2 },
            { x, r -> x > Long.MIN_VALUE && x < Long.MAX_VALUE && r == 4 },
            coverage = atLeast(75)
        )
    }

    @Test
    fun testUnreachableByteValue() {
        check(
            TypeBorders::unreachableByteValue,
            eq(1), // should generate one branch with legal byte value
            { x, r -> r == 0 && x < 200 },
            coverage = atLeast(50)
        )
    }
}