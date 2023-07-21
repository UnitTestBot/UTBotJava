package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType

import org.junit.jupiter.api.Test
import org.utbot.examples.spring.autowiring.SpringNoConfigUtValueTestCaseChecker
import org.utbot.examples.spring.utils.namePersonCall
import org.utbot.examples.spring.utils.agePersonCall
import org.utbot.examples.spring.utils.springAdditionalDependencies
import org.utbot.examples.spring.utils.springMockStrategy
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.testcheckers.eq
import org.utbot.testing.*

class PersonServiceTest : SpringNoConfigUtValueTestCaseChecker(
    testClass = PersonService::class,
) {


    /**
     *  In this test, we check the case when the Engine reproduces two models on two @Autowired variables of the same type.
     *
     *  Therefore, we mock all the variables and get the only necessary `mock.values` in each variable
     */
    @Test
    fun testJoin() {
        checkThisMocksAndExceptions(
            method = PersonService::joinInfo,
            branches = eq(2),
            { thisInstance, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMock("personTwo", agePersonCall)

                val personOneName = personOne.value<String?>()
                val personTwoAge = personTwo.value<Int?>().toString()

                personOneName + personTwoAge + thisInstance.baseOrders.size == r.getOrNull()
            },
            { thisInstance, mocks, r ->
                val personOne = mocks.singleMock("personOne", namePersonCall)
                val personTwo = mocks.singleMock("personTwo", agePersonCall)

                //Test conditions
                val r1 = (personOne != null)
                val r2 = (personTwo != null)
                val r3 = thisInstance.baseOrders == null

                r1 && r2 && r3 && r.isException<NullPointerException>()
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
            method = PersonService::ageSum,
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