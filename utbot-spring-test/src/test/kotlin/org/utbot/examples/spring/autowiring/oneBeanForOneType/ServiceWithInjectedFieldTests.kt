package org.utbot.examples.spring.autowiring.oneBeanForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.utils.findAllRepositoryCall
import org.utbot.examples.spring.utils.saveRepositoryCall
import org.utbot.examples.spring.utils.springAdditionalDependencies
import org.utbot.examples.spring.utils.springMockStrategy
import org.utbot.testcheckers.eq
import org.utbot.testing.*

internal class ServiceWithInjectedFieldTests : SpringNoConfigUtValueTestCaseChecker(
    testClass = ServiceWithInjectedField::class,
) {
    @Test
    fun testGetOrders() {
        checkMocks(
            method = ServiceWithInjectedField::getOrders,
            branches = eq(1),
            { mocks, r ->
                val orderRepository = mocks.singleMock("orderRepository", findAllRepositoryCall)
                orderRepository.value<List<Order>?>() == r
            },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }

    @Test
    fun testCreateOrder() {
        checkThisMocksAndExceptions(
            method = ServiceWithInjectedField::createOrder,
            // TODO: replace with `branches = eq(1)` after fix of https://github.com/UnitTestBot/UTBotJava/issues/2367
            branches = ignoreExecutionsNumber,
            { _, _, mocks, r: Result<Order?> ->
                val orderRepository = mocks.singleMock("orderRepository", saveRepositoryCall)
                orderRepository.value<Order?>() == r.getOrNull()
            },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }
}