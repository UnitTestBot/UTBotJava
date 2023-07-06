package org.utbot.examples.mock

import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.util.singleModel
import org.utbot.framework.util.singleStaticMethod
import org.utbot.framework.util.singleValue
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.id
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

// TODO Kotlin mocks generics https://github.com/UnitTestBot/UTBotJava/issues/88
internal class MockStaticMethodExampleTest : UtValueTestCaseChecker(
    testClass = MockStaticMethodExample::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testUseStaticMethod() {
        checkMocksAndInstrumentation(
            MockStaticMethodExample::useStaticMethod,
            eq(2),
            { _, instrumentation, r ->
                val mockValue = instrumentation
                    .singleStaticMethod("nextRandomInt")
                    .singleModel<UtPrimitiveModel>()
                    .singleValue() as Int

                mockValue > 50 && r == 100
            },
            { _, instrumentation, r ->
                val mockValue = instrumentation
                    .singleStaticMethod("nextRandomInt")
                    .singleModel<UtPrimitiveModel>()
                    .singleValue() as Int

                mockValue <= 50 && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }

    @Test
    fun testMockStaticMethodFromAlwaysMockClass() {
        checkMocksAndInstrumentation(
            MockStaticMethodExample::mockStaticMethodFromAlwaysMockClass,
            eq(1),
            coverage = DoNotCalculate,
            additionalMockAlwaysClasses = setOf(System::class.id)
        )
    }
}