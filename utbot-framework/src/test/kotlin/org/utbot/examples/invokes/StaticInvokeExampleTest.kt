package org.utbot.examples.invokes

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.between
import kotlin.math.max
import org.junit.jupiter.api.Test

internal class StaticInvokeExampleTest : UtTestCaseChecker(testClass = StaticInvokeExample::class) {
    // TODO: inline local variables when types inference bug in Kotlin fixed
    @Test
    fun testMaxForThree() {
        val method = StaticInvokeExample::maxForThree
        checkStaticMethod(
            method,
            between(2..3), // two executions can cover all branches
            { x, y, _, _ -> x > y },
            { x, y, _, _ -> x <= y },
            { x, y, z, _ -> max(x, y.toInt()) > z },
            { x, y, z, _ -> max(x, y.toInt()) <= z },
        )
    }
}