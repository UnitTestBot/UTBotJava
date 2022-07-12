package org.utbot.examples.wrappers

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class ShortWrapperTest : UtTestCaseChecker(testClass = ShortWrapper::class) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            ShortWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            ShortWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Disabled("Caching short values between -128 and 127 isn't supported JIRA:1481")
    @Test
    fun equalityTest() {
        check(
            ShortWrapper::equality,
            eq(3),
            { a, b, result -> a == b && a >= -128 && a <= 127 && result == 1 },
            { a, b, result -> a == b && (a < -128 || a > 127) && result == 2 },
            { a, b, result -> a != b && result == 4 },
            coverage = DoNotCalculate
        )
    }
}