package org.utbot.examples.mixed

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.atLeast
import org.utbot.examples.ignoreExecutionsNumber
import org.junit.jupiter.api.Test

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