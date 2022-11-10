package org.utbot.examples.threads

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import java.util.concurrent.ExecutionException

class FutureExamplesTest : UtValueTestCaseChecker(testClass = FutureExamples::class) {
    @Test
    fun testThrowingRunnable() {
        checkWithException(
            FutureExamples::throwingRunnableExample,
            eq(1),
            { r -> r.isException<ExecutionException>() },
            coverage = AtLeast(71)
        )
    }
}
