package org.utbot.examples.unsafe

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutSandbox
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

internal class UnsafeOperationsTest : UtValueTestCaseChecker(testClass = UnsafeOperations::class) {
    @Test
    fun checkGetAddressSizeOrZero() {
        withoutSandbox {
            check(
                UnsafeOperations::getAddressSizeOrZero,
                eq(1),
                { r -> r!! > 0 },
                // Coverage matcher fails (branches: 0/0, instructions: 15/21, lines: 0/0)
                // TODO: check coverage calculation: https://github.com/UnitTestBot/UTBotJava/issues/807
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun checkGetAddressSizeOrZeroWithMocks() {
        withoutSandbox {
            check(
                UnsafeOperations::getAddressSizeOrZero,
                eq(1),
                { r -> r!! > 0 },
                // Coverage matcher fails (branches: 0/0, instructions: 15/21, lines: 0/0)
                // TODO: check coverage calculation: https://github.com/UnitTestBot/UTBotJava/issues/807
                coverage = DoNotCalculate,
                mockStrategy = MockStrategyApi.OTHER_CLASSES
            )
        }
    }
}