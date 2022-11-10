package org.utbot.examples.mixed

import org.junit.jupiter.api.Test
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.atLeast
import org.utbot.testing.ignoreExecutionsNumber

internal class MonitorUsageTest : UtValueTestCaseChecker(testClass = MonitorUsage::class) {
    @Test
    fun testSimpleMonitor() {
        check(
            MonitorUsage::simpleMonitor,
            ignoreExecutionsNumber,
            { x, r -> x <= 0 && r == 0 },
            { x, r -> x > 0 && x <= Int.MAX_VALUE - 1 && r == 1 },
            coverage = atLeast(81) // differs from JaCoCo
        )
    }
}