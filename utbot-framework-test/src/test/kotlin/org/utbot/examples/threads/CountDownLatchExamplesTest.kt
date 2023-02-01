package org.utbot.examples.threads

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.AtLeast
import org.utbot.testing.UtValueTestCaseChecker

class CountDownLatchExamplesTest : UtValueTestCaseChecker(testClass = CountDownLatchExamples::class) {
    @Test
    fun testGetAndDown() {
        check(
            CountDownLatchExamples::getAndDown,
            eq(2),
            { countDownLatch, l -> countDownLatch.count == 0L && l == 0L },
            { countDownLatch, l ->
                val firstCount = countDownLatch.count

                firstCount != 0L && l == firstCount - 1
            },
            coverage = AtLeast(83)
        )
    }
}
