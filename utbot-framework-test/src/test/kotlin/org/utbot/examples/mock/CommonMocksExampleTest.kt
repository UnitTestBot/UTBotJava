package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class CommonMocksExampleTest: UtValueTestCaseChecker(testClass = CommonMocksExample::class) {
    @Test
    fun testMockInterfaceWithoutImplementors() {
        checkMocks(
            CommonMocksExample::mockInterfaceWithoutImplementors,
            eq(2),
            { v, mocks, _ -> v == null && mocks.isEmpty() },
            { _, mocks, _ -> mocks.singleOrNull() != null },
            coverage = DoNotCalculate
        )
    }

    // TODO JIRA:1449
    @Test
    fun testDoNotMockEquals() {
        checkMocks(
            CommonMocksExample::doNotMockEquals,
            eq(2),
            { fst, _, mocks, _ -> fst == null && mocks.isEmpty() },
            { _, _, mocks, _ -> mocks.isEmpty() }, // should be changed to not null fst when 1449 will be finished
            mockStrategy = MockStrategyApi.OTHER_PACKAGES,
            coverage = DoNotCalculate
        )
    }

    // TODO JIRA:1449
    @Test
    fun testNextValue() {
        checkMocks(
            CommonMocksExample::nextValue,
            eq(4),
            // node == null -> NPE
            // node.next == null -> NPE
            // node == node.next
            // node.next.value == node.value + 1
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testClinitMockExample() {
        check(
            CommonMocksExample::clinitMockExample,
            eq(1),
            { r -> r == -420 },
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            coverage = DoNotCalculate
        )
    }
}
