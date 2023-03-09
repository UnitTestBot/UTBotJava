package org.utbot.examples.mock

import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.atLeast

internal class CommonMocksExampleTest: UtValueTestCaseChecker(testClass = CommonMocksExample::class) {

    //TODO: coverage values here require further investigation by experts

    @Test
    fun testMockInterfaceWithoutImplementorsWithNoMocksStrategy() {
        checkMocks(
            CommonMocksExample::mockInterfaceWithoutImplementors,
            eq(1),
            { v, mocks, _ -> v == null && mocks.isEmpty() },
            mockStrategy = MockStrategyApi.NO_MOCKS,
            coverage = atLeast(75),
        )
    }

    @Test
    fun testMockInterfaceWithoutImplementorsWithMockingStrategy() {
        checkMocks(
            CommonMocksExample::mockInterfaceWithoutImplementors,
            eq(2),
            { v, mocks, _ -> v == null && mocks.isEmpty() },
            { _, mocks, _ -> mocks.singleOrNull() != null },
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            coverage = atLeast(75),
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
            coverage = atLeast(75)
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
            coverage = atLeast(13)
        )
    }

    @Test
    fun testClinitMockExample() {
        check(
            CommonMocksExample::clinitMockExample,
            eq(1),
            { r -> r == -420 },
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            coverage = atLeast(70),
        )
    }

    @Test
    fun testMocksForNullOfDifferentTypes() {
        check(
            CommonMocksExample::mocksForNullOfDifferentTypes,
            eq(1),
            mockStrategy = MockStrategyApi.OTHER_PACKAGES,
            coverage = atLeast(75)
        )
    }
}
