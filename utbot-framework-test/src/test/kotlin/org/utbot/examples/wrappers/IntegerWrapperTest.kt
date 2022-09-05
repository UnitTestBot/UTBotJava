package org.utbot.examples.wrappers

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class IntegerWrapperTest : UtValueTestCaseChecker(testClass = IntegerWrapper::class) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            IntegerWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            IntegerWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun numberOfZerosTest() {
        check(
            IntegerWrapper::numberOfZeros,
            eq(4),
            { x, _ -> x == null },
            { x, r -> Integer.numberOfLeadingZeros(x) >= 5 && r == 0 },
            { x, r -> Integer.numberOfLeadingZeros(x) < 5 && Integer.numberOfTrailingZeros(x) >= 5 && r == 0 },
            { x, r -> Integer.numberOfLeadingZeros(x) < 5 && Integer.numberOfTrailingZeros(x) < 5 && r == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun bitCountTest() {
        check(
            IntegerWrapper::bitCount,
            eq(3),
            { x, _ -> x == null },
            { x, r -> Integer.bitCount(x) != 5 && r == 0 },
            { x, r -> Integer.bitCount(x) == 5 && r == 1 },
            coverage = DoNotCalculate
        )
    }


    @Disabled("Caching integer values between -128 and 127 isn't supported JIRA:1481")
    @Test
    fun equalityTest() {
        check(
            IntegerWrapper::equality,
            eq(3),
            { a, b, result -> a == b && a >= -128 && a <= 127 && result == 1 },
            { a, b, result -> a == b && (a < -128 || a > 127) && result == 2 },
            { a, b, result -> a != b && result == 4 },
            coverage = DoNotCalculate
        )
    }

}