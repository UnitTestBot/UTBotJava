package org.utbot.examples.threads

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.withoutConcrete
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException

class ThreadStartExampleTest : UtValueTestCaseChecker(testClass = ThreadStartExample::class) {
    @Test
    // TODO minimization does not work
    fun testExceptionInStart() {
        // TODO concrete execution does not discover an exception - looks like it cannot find an exception in another thread
        withoutConcrete {
            // TODO an exception in another thread is not captured by assertThrows, we should find another way to support exceptions in different threads
            withEnabledTestingCodeGeneration(false) {
                checkWithException(
                    ThreadStartExample::explicitExceptionInStart,
                    ignoreExecutionsNumber,
                    { r -> r.isException<IllegalStateException>() }
                )
            }
        }
    }
}
