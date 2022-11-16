package org.utbot.examples.arrays

import org.junit.jupiter.api.Test
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber

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