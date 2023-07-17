package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.autowiring.OrderRepository
import org.utbot.examples.spring.utils.springAdditionalDependencies
import org.utbot.examples.spring.utils.standardSpringTestingConfigurations
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.singleMock
import org.utbot.testing.value
import kotlin.reflect.KFunction2
import kotlin.reflect.full.functions

internal class PersonServiceTest : UtValueTestCaseChecker(
    testClass = PersonService::class,
    configurations = standardSpringTestingConfigurations
) {
    @Test
    fun testGetOrders() {
        checkMocks(
            method = PersonService::join,
            branches = eq(1),
            { mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMock("personTwo", namePersonCall)
                personOne.value<String?>() + personTwo.value<String?>() == r
            },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_CLASSES,
            additionalDependencies = springAdditionalDependencies,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val namePersonCall: KFunction2<Person, Person, String?> =
        OrderRepository::class
            .functions
            .single { it.name == "compare" && it.parameters.size == 2 }
                as KFunction2<Person, Person, String?>
}