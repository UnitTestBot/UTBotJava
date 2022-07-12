package org.utbot.examples.mock

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.mock.others.Generator
import org.utbot.examples.mock.others.Locator
import org.utbot.examples.mockValue
import org.utbot.examples.singleMock
import org.utbot.examples.singleMockOrNull
import org.utbot.examples.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.junit.jupiter.api.Test

internal class MockReturnObjectExampleTest : UtTestCaseChecker(testClass = MockReturnObjectExample::class) {
    @Test
    fun testMockReturnObject() {
        checkMocks(
            MockReturnObjectExample::calculate,
            eq(6), // 4 NPE
            // NPE, privateLocator is null
            { _, mocks, r ->
                val privateLocator = mocks.singleMockOrNull("privateLocator", Locator::locate)
                privateLocator == null && r == null
            },
            // NPE, privateLocator.locate() returns null
            { _, mocks, r ->
                val generator = mocks.singleMock("privateLocator", Locator::locate).value<Generator?>()
                generator == null && r == null
            },
            // NPE, publicLocator is null
            { _, mocks, r ->
                val publicLocator = mocks.singleMockOrNull("publicLocator", Locator::locate)
                publicLocator == null && r == null
            },
            // NPE, publicLocator.locate() returns null
            { _, mocks, r ->
                val generator = mocks.singleMock("publicLocator", Locator::locate).value<Generator?>()
                generator == null && r == null
            },
            { threshold, mocks, r ->
                val mockId1 = mocks.singleMock("privateLocator", Locator::locate).mockValue().id
                val mockId2 = mocks.singleMock("publicLocator", Locator::locate).mockValue().id

                val mock1 = mocks.singleMock(mockId1, Generator::generateInt)
                val mock2 = mocks.singleMock(mockId2, Generator::generateInt)

                val (index1, index2) = if (mock1.values.size > 1) 0 to 1 else 0 to 0
                val value1 = mock1.value<Int>(index1)
                val value2 = mock2.value<Int>(index2)


                threshold < value1 + value2 && r == threshold
            },
            { threshold, mocks, r ->
                val mockId1 = mocks.singleMock("privateLocator", Locator::locate).mockValue().id
                val mockId2 = mocks.singleMock("publicLocator", Locator::locate).mockValue().id

                val mock1 = mocks.singleMock(mockId1, Generator::generateInt)
                val mock2 = mocks.singleMock(mockId2, Generator::generateInt)

                val (index1, index2) = if (mock1.values.size > 1) 0 to 1 else 0 to 0
                val value1 = mock1.value<Int>(index1)
                val value2 = mock2.value<Int>(index2)

                threshold >= value1 + value2 && r == value1 + value2 + 1
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }
}