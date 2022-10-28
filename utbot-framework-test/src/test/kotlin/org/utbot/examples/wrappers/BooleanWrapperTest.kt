package org.utbot.examples.wrappers

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withConcrete

internal class BooleanWrapperTest : UtValueTestCaseChecker(testClass = BooleanWrapper::class) {
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
        withConcrete(useConcreteExecution = true) { // Wrappers caches are not supported in symbolic analysis
            check(
                BooleanWrapper::equality,
                eq(2),
                { a, b, result -> a == b && result == 1 },
                { a, b, result -> a != b && result == 4 },
                coverage = DoNotCalculate // method under test has unreachable branches because of caching
            )
        }
    }
}