package org.utbot.examples.mixed

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.StaticInitializerExample
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class StaticInitializerExampleTest : AbstractTestCaseGeneratorTest(testClass = StaticInitializerExample::class) {
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