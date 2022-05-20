package org.utbot.examples.mixed

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class SimplifierTest: AbstractTestCaseGeneratorTest(testClass = Simplifier::class) {
    @Test
    fun testSimplifyAdditionWithZero() {
        check(
            Simplifier::simplifyAdditionWithZero,
            eq(1),
            { fst, r -> r != null && r.x == fst.shortValue.toInt() },
            coverage = DoNotCalculate // because of assumes
        )
    }
}