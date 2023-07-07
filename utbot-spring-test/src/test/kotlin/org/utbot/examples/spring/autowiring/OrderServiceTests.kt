package org.utbot.examples.spring.autowiring

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.utils.springAdditionalDependencies
import org.utbot.examples.spring.utils.standardSpringTestingConfigurations
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testcheckers.eq
import org.utbot.testing.*
import kotlin.reflect.full.functions
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

internal class OrderServiceTests : UtValueTestCaseChecker(
    testClass = OrderService::class,
    configurations = standardSpringTestingConfigurations
) {
    @Test
    fun testGetOrders() {
        checkMocks(
            method = OrderService::getOrders,
            branches = eq(1),
            { mocks, r ->
                val orderRepository = mocks.singleMock("orderRepository", findAllRepositoryCall)
                orderRepository.value<List<Order>?>() == r
            },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            additionalDependencies = springAdditionalDependencies,
        )
    }

    @Test
    fun testCreateOrder() {
        checkMocksWithExceptions(
            method = OrderService::createOrder,
            // TODO: replace with `branches = eq(1)` after fix of https://github.com/UnitTestBot/UTBotJava/issues/2367
            branches = ignoreExecutionsNumber,
            { _: Order?, mocks, r: Result<Order?> ->
                val orderRepository = mocks.singleMock("orderRepository", saveRepositoryCall)
                orderRepository.value<Order?>() == r.getOrNull()
            },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            additionalDependencies = springAdditionalDependencies,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val findAllRepositoryCall: KFunction1<OrderRepository, List<Order>?> =
        OrderRepository::class
            .functions
            .single { it.name == "findAll" && it.parameters.size == 1 }
                as KFunction1<OrderRepository, List<Order>?>


    @Suppress("UNCHECKED_CAST")
    private val saveRepositoryCall: KFunction2<OrderRepository, Order?, Order?> =
        OrderRepository::class
            .functions
            .single { it.name == "save" && it.parameters.size == 2 }
                as KFunction2<OrderRepository, Order?, Order?>
}