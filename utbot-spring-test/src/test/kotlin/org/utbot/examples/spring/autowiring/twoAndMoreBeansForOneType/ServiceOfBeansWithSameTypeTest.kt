package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.autowiring.SpringNoConfigUtValueTestCaseChecker
import org.utbot.examples.spring.utils.*
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.testcheckers.eq
import org.utbot.testing.*

class ServiceOfBeansWithSameTypeTest : SpringNoConfigUtValueTestCaseChecker(
    testClass = ServiceOfBeansWithSameType::class,
) {

    /**
     *  In this test, we check the case when the Engine reproduces two models on two @Autowired variables of the same type.
     *
     *  Therefore, we mock all the variables and get the only necessary `mock.values` in each variable
     */
    @Test
    fun testJoin() {
        checkThisMocksAndExceptions(
            method = ServiceOfBeansWithSameType::joinInfo,
            branches = eq(3),
            { _, mocks, r ->
                val personOne = mocks.singleMock("personOne", weightPersonCall)

                val personOneWeight = personOne.value<Int?>()

                val isPersonOneWeightNull = personOneWeight == null

                isPersonOneWeightNull && r.isException<NullPointerException>()
            },
            { _, mocks, r ->
                val personOne = mocks.singleMock("personOne", weightPersonCall)
                val personTwo = mocks.singleMock("personTwo", agePersonCall)

                val personOneWeight = personOne.value<Int?>()
                val personTwoAge = personTwo.value<Int?>()

                val isPersonOneWeightNotNull = personOneWeight != null
                val isPersonTwoAgeNull = personTwoAge == null

                isPersonOneWeightNotNull && isPersonTwoAgeNull && r.isException<NullPointerException>()
            },
            { thisInstance, mocks, r ->
                val personOne = mocks.singleMock("personOne", weightPersonCall)
                val personTwo = mocks.singleMock("personTwo", agePersonCall)

                val personOneWeight = personOne.value<Int?>()!!
                val personTwoAge = personTwo.value<Int?>()!!
                val baseOrders = thisInstance.baseOrders!!

                personOneWeight + personTwoAge + baseOrders.size == r.getOrNull()
            },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }

    /**
     *  In this test, we check the case when the Engine reproduces one model on two @Autowired variables of the same type.
     *
     *  Therefore, we only mock one of the variables and get all `mock.values` in it
     */
    @Test
    fun testAgeSum(){
        checkThisMocksAndExceptions(
            method = ServiceOfBeansWithSameType::ageSum,
            branches = ignoreExecutionsNumber,
            { _, mocks, r ->
                val personOne = mocks.singleMock("personOne", agePersonCall)

                val personOneAge = (personOne.values[0] as? UtConcreteValue<*>)?.value
                val isPersonOneAgeNull = personOneAge == null

                isPersonOneAgeNull && r.isException<NullPointerException>()
            },
            { _, mocks, r ->
                val personOne = mocks.singleMock("personOne", agePersonCall)

                val personOneAge = (personOne.values[0] as? UtConcreteValue<*>)?.value
                val personTwoAge = (personOne.values[1] as? UtConcreteValue<*>)?.value

                val isPersonOneAgeNull = personOneAge != null
                val isPersonTwoAgeNotNull = personTwoAge == null

                isPersonOneAgeNull && isPersonTwoAgeNotNull && r.isException<NullPointerException>()
            },
            { _, mocks, r ->
                val personOne = mocks.singleMock("personOne", agePersonCall)

                val personOneAge = (personOne.values[0] as? UtConcreteValue<*>)?.value.toString().toInt()
                val personTwoAge = (personOne.values[1] as? UtConcreteValue<*>)?.value.toString().toInt()

                personOneAge + personTwoAge == r.getOrNull()
            },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }
}
