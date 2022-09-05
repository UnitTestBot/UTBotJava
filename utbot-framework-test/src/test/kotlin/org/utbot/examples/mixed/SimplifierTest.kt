package org.utbot.examples.mixed

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class SimplifierTest: UtValueTestCaseChecker(testClass = Simplifier::class) {
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