package org.utbot.examples.mock

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.ge
import org.utbot.examples.mock.others.FinalClass
import org.utbot.examples.singleMock
import org.utbot.examples.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_CLASSES
import org.junit.jupiter.api.Test

internal class MockFinalClassTest : UtValueTestCaseChecker(testClass = MockReturnObjectExample::class) {
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