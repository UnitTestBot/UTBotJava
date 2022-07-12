package org.utbot.examples.mock

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.isException
import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.Test

internal class MockWithSideEffectExampleTest : UtValueTestCaseChecker(testClass = MockWithSideEffectExample::class) {
    @Test
    fun testSideEffect() {
        checkWithException(
            MockWithSideEffectExample::checkSideEffect,
            eq(3),
            { _, r -> r.isException<NullPointerException>() },
            { _, r -> r.getOrNull() == false },
            { _, r -> r.getOrNull() == true },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }

    @Test
    fun testSideEffectWithoutMocks() {
        checkWithException(
            MockWithSideEffectExample::checkSideEffect,
            eq(2),
            { _, r -> r.isException<NullPointerException>() },
            { _, r -> r.getOrNull() == true },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.NO_MOCKS
        )
    }

    @Test
    fun testSideEffectElimination() {
        checkWithException(
            MockWithSideEffectExample::checkSideEffectElimination,
            eq(1),
            { _, r -> r.getOrNull() == true },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }

    @Test
    fun testStaticMethodSideEffectElimination() {
        checkWithException(
            MockWithSideEffectExample::checkStaticMethodSideEffectElimination,
            eq(1),
            { _, r -> r.getOrNull() == true },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }

    @Test
    fun testStaticMethodSideEffectEliminationWithoutMocks() {
        checkWithException(
            MockWithSideEffectExample::checkStaticMethodSideEffectElimination,
            eq(1),
            { _, r -> r.getOrNull() == false },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.NO_MOCKS
        )
    }

}