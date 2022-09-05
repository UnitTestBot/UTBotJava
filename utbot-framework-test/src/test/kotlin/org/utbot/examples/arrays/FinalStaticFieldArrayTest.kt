package org.utbot.examples.arrays

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.junit.jupiter.api.Test

internal class FinalStaticFieldArrayTest : UtValueTestCaseChecker(testClass = FinalStaticFieldArray::class) {

    @Test
    fun testFactorial() {
        checkStaticMethod(
            FinalStaticFieldArray::factorial,
            ignoreExecutionsNumber,
            { n, r ->
                (n as Int) >= 0 && n < FinalStaticFieldArray.MAX_FACTORIAL && r == FinalStaticFieldArray.factorial(n)
            },
            { n, _ -> (n as Int) < 0 },
            { n, r -> (n as Int) > FinalStaticFieldArray.MAX_FACTORIAL && r == FinalStaticFieldArray.factorial(n) },
        )
    }
}