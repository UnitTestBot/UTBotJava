package org.utbot.examples.objects

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.Test

internal class SimpleClassMultiInstanceExampleTest : UtValueTestCaseChecker(testClass = SimpleClassMultiInstanceExample::class) {
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