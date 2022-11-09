package org.utbot.examples.mock

import org.utbot.examples.mock.others.FinalClass
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_CLASSES
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.ge
import org.utbot.testing.*

internal class MockFinalClassTest : UtValueTestCaseChecker(
    testClass = MockFinalClassExample::class,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN)
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