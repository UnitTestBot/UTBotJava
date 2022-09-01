package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.examples.mock.others.FinalClass
import org.utbot.tests.infrastructure.singleMock
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_CLASSES
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.ge

internal class MockFinalClassTest : UtValueTestCaseChecker(testClass = MockFinalClassExample::class) {
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