package org.utbot.examples.mixed

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.StaticInitializerExample
import org.utbot.examples.eq

@Disabled("Unknown build failure")
internal class StaticInitializerExampleTest : UtTestCaseChecker(testClass = StaticInitializerExample::class) {
    @Test
    fun testPositive() {
        check(
            StaticInitializerExample::positive,
            eq(2),
            { i, r -> i > 0 && r == true },
            { i, r -> i <= 0 && r == false },
        )
    }

    @Test
    fun testNegative() {
        check(
            StaticInitializerExample::negative,
            eq(2),
            { i, r -> i < 0 && r == true },
            { i, r -> i >= 0 && r == false },
        )
    }
}