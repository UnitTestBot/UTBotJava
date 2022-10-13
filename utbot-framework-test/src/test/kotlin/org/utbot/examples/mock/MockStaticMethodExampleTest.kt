package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.util.singleModel
import org.utbot.framework.util.singleStaticMethod
import org.utbot.framework.util.singleValue
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.Compilation
import org.utbot.tests.infrastructure.TestExecution

// TODO Kotlin mocks generics https://github.com/UnitTestBot/UTBotJava/issues/88
internal class MockStaticMethodExampleTest : UtValueTestCaseChecker(
    testClass = MockStaticMethodExample::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA, lastStage = TestExecution, parameterizedModeLastStage = Compilation),
        TestLastStage(CodegenLanguage.KOTLIN, lastStage = CodeGeneration)
    )
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
}