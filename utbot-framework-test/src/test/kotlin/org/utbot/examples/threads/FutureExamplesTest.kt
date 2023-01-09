package org.utbot.examples.threads

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.AtLeast
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException
import java.util.concurrent.ExecutionException

// IMPORTANT: most of the these tests test only the symbolic engine
// and should not be used for testing conrete or code generation since they are possibly flaky in the concrete execution
// (see https://github.com/UnitTestBot/UTBotJava/issues/1610)
class FutureExamplesTest : UtValueTestCaseChecker(testClass = FutureExamples::class) {
    @Test
    fun testThrowingRunnable() {
        withoutConcrete {
            checkWithException(
                FutureExamples::throwingRunnableExample,
                eq(1),
                { r -> r.isException<ExecutionException>() },
                coverage = AtLeast(71)
            )
        }
    }

    @Test
    fun testResultFromGet() {
        check(
            FutureExamples::resultFromGet,
            eq(1),
            { r -> r == 42 },
        )
    }

    @Test
    fun testChangingCollectionInFuture() {
        withEnabledTestingCodeGeneration(false) {
            check(
                FutureExamples::changingCollectionInFuture,
                eq(1),
                { r -> r == 42 },
            )
        }
    }

    @Test
    fun testChangingCollectionInFutureWithoutGet() {
        withoutConcrete {
            withEnabledTestingCodeGeneration(false) {
                check(
                    FutureExamples::changingCollectionInFutureWithoutGet,
                    eq(1),
                    { r -> r == 42 },
                    coverage = AtLeast(78)
                )
            }
        }
    }
}
