package org.utbot.examples.math

import org.junit.jupiter.api.Disabled
import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class OverflowExamplesTest : AbstractTestCaseGeneratorTest(testClass = OverflowExamples::class) {
    @Test
    @Disabled("TODO move to existing test file")
    fun testIntOverflow() {
        check(
            OverflowExamples::intOverflow,
            eq(6),
            { x, _, r -> x * x * x <= 0 && x <= 0 && r == 0 },
            { x, _, r -> x * x * x > 0 && x <= 0 && r == 0 }, // through overflow
            { x, y, r -> x * x * x > 0 && x > 0 && y != 10 && r == 0 },
            { x, y, r -> x * x * x > 0 && x > 0 && y == 10 && r == 1 },
            { x, y, r -> x * x * x <= 0 && x > 0 && y != 20 && r == 0 }, // through overflow
            { x, y, r -> x * x * x <= 0 && x > 0 && y == 20 && r == 2 } // through overflow
        )
    }
}
