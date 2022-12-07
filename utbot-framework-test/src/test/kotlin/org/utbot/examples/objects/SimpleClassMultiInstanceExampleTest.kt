package org.utbot.examples.objects

import org.junit.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class SimpleClassMultiInstanceExampleTest : UtValueTestCaseChecker(testClass =
    SimpleClassMultiInstanceExample::class) {
    @Test
    fun singleObjectChangeTest() {
        check(
            SimpleClassMultiInstanceExample::singleObjectChange,
            eq(3),
            { first, _, _ -> first == null }, // NPE
            { first, _, r -> first.a < 5 && r == 3 },
            { first, _, r -> first.a >= 5 && r == first.b },
            coverage = DoNotCalculate
        )
    }
}