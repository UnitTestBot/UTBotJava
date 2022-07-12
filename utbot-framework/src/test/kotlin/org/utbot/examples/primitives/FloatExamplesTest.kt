package org.utbot.examples.primitives

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class FloatExamplesTest : UtTestCaseChecker(testClass = FloatExamples::class) {
    @Test
    fun testFloatInfinity() {
        check(
            FloatExamples::floatInfinity,
            eq(3),
            { f, r -> f == Float.POSITIVE_INFINITY && r == 1 },
            { f, r -> f == Float.NEGATIVE_INFINITY && r == 2 },
            { f, r -> !f.isInfinite() && r == 3 },
        )
    }
}