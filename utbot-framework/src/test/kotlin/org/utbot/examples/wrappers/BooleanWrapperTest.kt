package org.utbot.examples.wrappers

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class BooleanWrapperTest : UtTestCaseChecker(testClass = BooleanWrapper::class) {
    @Test
    fun primitiveToWrapperTest() {
        check(
            BooleanWrapper::primitiveToWrapper,
            eq(2),
            { x, r -> x && r == true },
            { x, r -> !x && r == true },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun wrapperToPrimitiveTest() {
        check(
            BooleanWrapper::wrapperToPrimitive,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x && r == true },
            { x, r -> !x && r == true },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun equalityTest() {
        check(
            BooleanWrapper::equality,
            eq(2),
            { a, b, result -> a == b && result == 1 },
            { a, b, result -> a != b && result == 4 },
            coverage = DoNotCalculate // method under test has unreachable branches because of caching
        )
    }
}