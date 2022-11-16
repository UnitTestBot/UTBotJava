package org.utbot.examples.codegen

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException

class JavaAssertTest : UtValueTestCaseChecker(
    testClass = JavaAssert::class,
    testCodeGeneration = false
) {
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