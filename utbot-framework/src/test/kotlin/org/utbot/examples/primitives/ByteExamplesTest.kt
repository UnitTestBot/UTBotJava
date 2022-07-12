package org.utbot.examples.primitives

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class ByteExamplesTest : UtTestCaseChecker(testClass = ByteExamples::class) {
    @Test
    fun testNegByte() {
        check(
            ByteExamples::negByte,
            eq(2),
            { b, r -> b > 0 && r == 0 },
            { b, r -> b <= 0 && r == 1 },
        )
    }

    @Test
    fun testNegConstByte() {
        check(
            ByteExamples::negConstByte,
            eq(3),
            { b, r -> b <= -10 && r == 1 },
            { b, r -> b in -9..9 && r == 0 },
            { b, r -> b >= 10 && r == 1 },
        )
    }

    @Test
    fun testSumTwoBytes() {
        check(
            ByteExamples::sumTwoBytes,
            eq(3),
            { a, b, r -> a + b > Byte.MAX_VALUE && r == 1 },
            { a, b, r -> a + b < Byte.MIN_VALUE && r == 2 },
            { a, b, r -> a + b in Byte.MIN_VALUE..Byte.MAX_VALUE && r == 3 },
        )
    }
}