package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.autowiring.SpringNoConfigUtValueTestCaseChecker
import org.utbot.examples.spring.utils.agePersonCall
import org.utbot.examples.spring.utils.namePersonCall
import org.utbot.examples.spring.utils.springAdditionalDependencies
import org.utbot.examples.spring.utils.springMockStrategy
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.isException
import org.utbot.testing.singleMock
import org.utbot.testing.value

class ServiceOfBeansWithSameTypeTest : SpringNoConfigUtValueTestCaseChecker(
    testClass = ServiceOfBeansWithSameType::class,
) {

    /**
     *  In this test, we check the case when the Engine produces two models on two @Autowired variables of the same type.
     *
     *  The engine produce two models only in the tests, when `baseOrder` is a testing participant.
     *  In these tests, we mock all the variables and get the only necessary `mock.values` in each variable.
     */
    @Test
    fun testChecker() {
        checkThisMocksAndExceptions(
            method = ServiceOfBeansWithSameType::checker,
            branches = eq(6),
            {_, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)

                val personOneName = personOne.value<String?>()

                val r1 = personOneName == null

                r1 && r.isException<NullPointerException>()
            },
            {_, mocks, r ->
                val person = mocks.singleMock("personOne", namePersonCall)

                val personOneName = (person.values[0] as? UtConcreteValue<*>)?.value
                val personTwoName = (person.values[1] as? UtConcreteValue<*>)?.value

                val r1 = personOneName == "k"
                val r2 = personTwoName == null

                r1 && r2 && r.isException<NullPointerException>()
            },
            {_, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)

                val personOneName = personOne.value<String?>()

                val r1 = personOneName != "k"

                r1 && (r.getOrNull() == false)
            },
            {_, mocks, r ->
                val person = mocks.singleMock("personOne", namePersonCall)

                val personOneName = (person.values[0] as? UtConcreteValue<*>)?.value
                val personTwoName = (person.values[1] as? UtConcreteValue<*>)?.value.toString()

                val r1 = personOneName == "k"
                val r2 = personTwoName.length <= 5

                r1 && r2 && (r.getOrNull() == false)
            },

            //In this test Engine produces two models on two @Autowired variables of the same type
            {thisInstance, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMock("personTwo", namePersonCall)

                val personOneName = (personOne.values[0] as? UtConcreteValue<*>)?.value
                val personTwoName = (personTwo.values[0] as? UtConcreteValue<*>)?.value.toString()
                val baseOrders = thisInstance.baseOrders

                val r1 = personOneName == "k"
                val r2 = personTwoName.length > 5
                val r3 = baseOrders.isEmpty()

                r1 && r2 && r3 && (r.getOrNull() == true)
            },

            {thisInstance, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMock("personTwo", namePersonCall)

                val personOneName = (personOne.values[0] as? UtConcreteValue<*>)?.value
                val personTwoName = (personTwo.values[0] as? UtConcreteValue<*>)?.value.toString()
                val baseOrders = thisInstance.baseOrders

                val r1 = personOneName == "k"
                val r2 = personTwoName.length > 5
                val r3 = baseOrders.isNotEmpty()

                r1 && r2 && r3 && (r.getOrNull() == false)
            },
            coverage = DoNotCalculate,
            mockStrategy = springMockStrategy,
            additionalDependencies = springAdditionalDependencies,
        )
    }

    /**
     *  In this test, we check the case when the Engine produces one model on two @Autowired variables of the same type.
     *
     *  Therefore, we only mock one of the variables and get all `mock.values` in it
     */
    @Test
    fun testAgeSum(){
        checkThisMocksAndExceptions(
            method = ServiceOfBeansWithSameType::ageSum,
            branches = eq(3),
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
