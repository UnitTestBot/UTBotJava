package org.utbot.examples.lambda

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

class ThrowingWithLambdaExampleTest : UtValueTestCaseChecker(testClass = ThrowingWithLambdaExample::class) {
    @Test
    fun testAnyExample() {
        check(
            ThrowingWithLambdaExample::anyExample,
            eq(4),
            { l, _, _ -> l == null },
            { l, _, r -> l.isEmpty() && r == false },
            { l, _, r -> l.isNotEmpty() && 42 in l && r == true },
            { l, _, r -> l.isNotEmpty() && 42 !in l && r == false },
            coverage = DoNotCalculate // TODO failed coverage calculation
        )
    }
}
