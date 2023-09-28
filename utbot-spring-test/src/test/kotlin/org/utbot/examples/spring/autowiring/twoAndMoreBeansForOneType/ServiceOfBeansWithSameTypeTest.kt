package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.autowiring.SpringNoConfigUtValueTestCaseChecker
import org.utbot.examples.spring.utils.*
import org.utbot.testcheckers.eq
import org.utbot.testing.*

class ServiceOfBeansWithSameTypeTest : SpringNoConfigUtValueTestCaseChecker(
    testClass = ServiceOfBeansWithSameType::class,
) {

    /**
     *  In this test, we check  both cases when the Engine produces
     *    - two models for two @Autowired fields of the same type
     *    - one model for two @Autowired fields of the same type
     */
    @Test
    fun testChecker() {
        checkThisMocksAndExceptions(
            method = ServiceOfBeansWithSameType::checker,
            branches = eq(3),
            {_, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)

                val personOneName = personOne.value<String?>()

                val r1 = personOneName == null

                r1 && r.isException<NullPointerException>()
            },
            {_, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMockOrNull("personTwo", namePersonCall)

                val personOneName = personOne.value<String?>()

                val r1 = personOneName != null
                val r2 = personTwo == null // `personTwo.getName()` isn't mocked, meaning `personOne != personTwo`

                r1 && r2 && (r.getOrNull() == false)
            },
            {_, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMockOrNull("personTwo", namePersonCall)

                val personOneName = personOne.value<String?>()

                val r1 = personOneName != null
                val r2 = personTwo != null // `personTwo.getName()` is mocked, meaning `personOne == personTwo`

                r1 && r2 && (r.getOrNull() == true)
            },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }
}
