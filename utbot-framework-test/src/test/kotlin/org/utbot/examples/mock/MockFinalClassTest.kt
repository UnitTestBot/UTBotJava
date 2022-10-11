package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.examples.mock.others.FinalClass
import org.utbot.tests.infrastructure.singleMock
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_CLASSES
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.ge
import org.utbot.tests.infrastructure.Compilation
import org.utbot.tests.infrastructure.TestExecution

internal class MockFinalClassTest : UtValueTestCaseChecker(
    testClass = MockFinalClassExample::class,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA, lastStage = TestExecution, parameterizedModeLastStage = Compilation),
        TestLastStage(CodegenLanguage.KOTLIN, lastStage = TestExecution)
    )
) {
    @Test
    fun testFinalClass() {
        checkMocks(
            MockFinalClassExample::useFinalClass,
            ge(2),
            { mocks, r ->
                val intProvider = mocks.singleMock("intProvider", FinalClass::provideInt)
                intProvider.value<Int>(0) == 1 && r == 1
            },
            { mocks, r ->
                val intProvider = mocks.singleMock("intProvider", FinalClass::provideInt)
                intProvider.value<Int>(0) != 1 && r == 2
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_CLASSES
        )
    }
}