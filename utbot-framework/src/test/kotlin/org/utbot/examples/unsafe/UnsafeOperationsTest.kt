package org.utbot.examples.unsafe

import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.examples.withoutSandbox
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi

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
