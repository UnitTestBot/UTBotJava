package org.utbot.examples.wrappers

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class ByteWrapperTest : UtValueTestCaseChecker(testClass = ByteWrapper::class) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            ByteWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            ByteWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x >= 0 && r!! <= 0 },
            { x, r -> x < 0 && r!! < 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun equalityTest() {
        check(
            ByteWrapper::equality,
            eq(2),
            { a, b, result -> a == b && result == 1 },
            { a, b, result -> a != b && result == 4 },
            coverage = DoNotCalculate // method under test has unreachable branches because of caching
        )
    }
}