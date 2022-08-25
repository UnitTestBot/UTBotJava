package org.utbot.examples.exceptions

import org.utbot.tests.infrastructure.UtModelTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.primitiveValue
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtTimeoutException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.ge

internal class ExceptionClusteringChecker :
    UtModelTestCaseChecker(testClass = ExceptionClusteringExamples::class) {
    /**
     * Difference is in throwing unchecked exceptions - for method under test is [UtExpectedCheckedThrow].
     */
    @Test
    fun testDifferentExceptions() {
        check(
            ExceptionClusteringExamples::differentExceptions,
            ignoreExecutionsNumber,
            { i, r -> i.int() == 0 && r is UtImplicitlyThrownException && r.exception is ArithmeticException },
            { i, r -> i.int() == 1 && r is UtExplicitlyThrownException && r.exception is MyCheckedException },
            { i, r -> i.int() == 2 && r is UtExplicitlyThrownException && r.exception is IllegalArgumentException },
            { i, r -> i.int() !in 0..2 && r is UtExecutionSuccess && r.model.int() == 2 * i.int() },
        )
    }

    /**
     * Difference is in throwing unchecked exceptions - for nested call it is [UtUnexpectedUncheckedThrow].
     */
    @Test
    fun testDifferentExceptionsInNestedCall() {
        check(
            ExceptionClusteringExamples::differentExceptionsInNestedCall,
            ignoreExecutionsNumber,
            { i, r -> i.int() == 0 && r is UtImplicitlyThrownException && r.exception is ArithmeticException },
            { i, r -> i.int() == 1 && r is UtExplicitlyThrownException && r.exception is MyCheckedException },
            { i, r -> i.int() == 2 && r is UtExplicitlyThrownException && r.exception is IllegalArgumentException },
            { i, r -> i.int() !in 0..2 && r is UtExecutionSuccess && r.model.int() == 2 * i.int() },
        )
    }

    @Test
    fun testSleepingMoreThanDefaultTimeout() {
        check(
            ExceptionClusteringExamples::sleepingMoreThanDefaultTimeout,
            ge(1),
            { _, r -> r is UtTimeoutException }, // we will minimize one of these: i <= 0 or i > 0
        )
    }
}

private fun UtModel.int(): Int = this.primitiveValue()

