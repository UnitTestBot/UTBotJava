package org.utbot.examples.mock.aliasing

import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class AliasingInParamsExampleTest : UtValueTestCaseChecker(testClass = AliasingInParamsExample::class) {
    @Test
    fun testExamplePackageBased() {
        check(
            AliasingInParamsExample::example,
            eq(1),
            { fst, snd, x, r -> fst != snd && x == r },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }

    @Test
    fun testExample() {
        check(
            AliasingInParamsExample::example,
            eq(2),
            { fst, snd, x, r -> fst == snd && x == r },
            { fst, snd, x, r -> fst != snd && x == r },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.NO_MOCKS
        )
    }

}