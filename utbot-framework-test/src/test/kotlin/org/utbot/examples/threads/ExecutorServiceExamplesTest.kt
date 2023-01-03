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
class ExecutorServiceExamplesTest : UtValueTestCaseChecker(testClass = ExecutorServiceExamples::class) {
    @Test
    fun testExceptionInExecute() {
        withoutConcrete {
            withEnabledTestingCodeGeneration(false) {
                checkWithException(
                    ExecutorServiceExamples::throwingInExecute,
                    ignoreExecutionsNumber,
                    { r -> r.isException<IllegalStateException>() }
                )
            }
        }
    }

    @Test
    fun testChangingCollectionInExecute() {
        withoutConcrete {
            withEnabledTestingCodeGeneration(false) {
                check(
                    ExecutorServiceExamples::changingCollectionInExecute,
                    ignoreExecutionsNumber,
                    { r -> r == 42 },
                    coverage = AtLeast(78)
                )
            }
        }
    }
}
