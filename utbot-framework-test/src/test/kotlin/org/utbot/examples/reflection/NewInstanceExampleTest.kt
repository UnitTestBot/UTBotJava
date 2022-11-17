package org.utbot.examples.reflection

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException

class NewInstanceExampleTest : UtValueTestCaseChecker(NewInstanceExample::class, testCodeGeneration = true) {
    @Test
    fun testNewInstanceExample() {
        check(
            NewInstanceExample::createWithReflectionExample,
            eq(1),
            { r -> r == 0 }
        )
    }

    /*@Test
    fun testNewInstanceWithExceptionExample() {
        checkWithException(
            NewInstanceExample::createWithReflectionWithExceptionExample,
            eq(1),
            { r -> r.isException<InstantiationException>() },
            coverage = DoNotCalculate // an exception is thrown in the middle of the MUT
        )
    }*/
}
