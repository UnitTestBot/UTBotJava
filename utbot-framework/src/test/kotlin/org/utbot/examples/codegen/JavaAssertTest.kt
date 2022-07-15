package org.utbot.examples.codegen

import org.junit.jupiter.api.Test
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.examples.isException

class JavaAssertTest : UtValueTestCaseChecker(testClass = JavaAssert::class){
    @Test
    fun testAssertPositive() {
        checkWithException(
            JavaAssert::assertPositive,
            eq(2),
            { value, result -> value > 0 && result.isSuccess && result.getOrNull() == value },
            { value, result -> value <= 0 && result.isException<java.lang.AssertionError>() }
        )
    }
}