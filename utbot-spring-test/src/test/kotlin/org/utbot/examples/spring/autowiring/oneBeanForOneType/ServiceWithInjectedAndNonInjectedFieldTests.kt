package org.utbot.examples.spring.autowiring.oneBeanForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.autowiring.SpringNoConfigUtValueTestCaseChecker
import org.utbot.examples.spring.utils.findAllRepositoryCall
import org.utbot.examples.spring.utils.springAdditionalDependencies
import org.utbot.examples.spring.utils.springMockStrategy
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException
import org.utbot.testing.singleMock
import org.utbot.testing.value

internal class ServiceWithInjectedAndNonInjectedFieldTests : SpringNoConfigUtValueTestCaseChecker(
    testClass = ServiceWithInjectedAndNonInjectedField::class,
) {
    @Test
    fun testGetOrdersSize() {
        checkThisMocksAndExceptions(
            method = ServiceWithInjectedAndNonInjectedField::getOrdersSize,
            // TODO: replace with `branches = eq(3)`
            // after the fix of `speculativelyCannotProduceNullPointerException` in SpringApplicationContext
            branches = ignoreExecutionsNumber,
            { thisInstance, mocks, r: Result<Int> ->
                val orderRepository = mocks.singleMock("orderRepository", findAllRepositoryCall)
                val repositorySize =  orderRepository.value<List<Order>?>()!!.size
                repositorySize + thisInstance.selectedOrders.size == r.getOrNull()
            },
            { _, _, r: Result<Int> -> r.isException<NullPointerException>() },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }
}