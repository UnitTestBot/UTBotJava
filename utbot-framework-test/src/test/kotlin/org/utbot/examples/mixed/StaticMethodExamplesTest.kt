package org.utbot.examples.mixed

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class StaticMethodExamplesTest : UtValueTestCaseChecker(testClass = StaticMethodExamples::class) {
    // TODO: inline local variables when types inference bug in Kotlin fixed
    @Test
    fun testComplement() {
        val method = StaticMethodExamples::complement
        checkStaticMethod(
            method,
            eq(2),
            { x, r -> x == -2 && r == true },
            { x, r -> x != -2 && r == false }
        )
    }

    @Test
    fun testMax2() {
        val method = StaticMethodExamples::max2
        checkStaticMethod(
            method,
            eq(2),
            { x, y, r -> x > y && r == x },
            { x, y, r -> x <= y && r == y.toInt() }
        )
    }

    @Test
    fun testSum() {
        val method = StaticMethodExamples::sum
        checkStaticMethod(
            method,
            eq(3),
            { x, y, z, r -> x + y + z < -20 && r == (x + y + z).toLong() * 2 },
            { x, y, z, r -> x + y + z > 20 && r == (x + y + z).toLong() * 2 },
            { x, y, z, r -> x + y + z in -20..20 && r == (x + y + z).toLong() }
        )
    }
}