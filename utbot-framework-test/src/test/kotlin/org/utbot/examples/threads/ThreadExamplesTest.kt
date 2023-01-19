package org.utbot.examples.threads

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.AtLeast
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

// IMPORTANT: most of the these tests test only the symbolic engine
// and should not be used for testing conrete or code generation since they are possibly flaky in the concrete execution
// (see https://github.com/UnitTestBot/UTBotJava/issues/1610)
class ThreadExamplesTest : UtValueTestCaseChecker(testClass = ThreadExamples::class) {
    @Test
    fun testExceptionInStart() {
        withoutConcrete {
            withEnabledTestingCodeGeneration(false) {
                checkWithException(
                    ThreadExamples::explicitExceptionInStart,
                    ignoreExecutionsNumber,
                    { r -> r.isException<IllegalStateException>() }
                )
            }
        }
    }

    @Test
    fun testChangingCollectionInThread() {
        withoutConcrete {
            withEnabledTestingCodeGeneration(false) {
                check(
                    ThreadExamples::changingCollectionInThread,
                    ignoreExecutionsNumber,
                    { r -> r == 42 },
                    coverage = AtLeast(81)
                )
            }
        }
    }

    @Test
    fun testChangingCollectionInThreadWithoutStart() {
        withoutConcrete {
            withEnabledTestingCodeGeneration(false) {
                checkWithException(
                    ThreadExamples::changingCollectionInThreadWithoutStart,
                    ignoreExecutionsNumber,
                    { r -> r.isException<IndexOutOfBoundsException>() },
                    coverage = AtLeast(81)
                )
            }
        }
    }
}
