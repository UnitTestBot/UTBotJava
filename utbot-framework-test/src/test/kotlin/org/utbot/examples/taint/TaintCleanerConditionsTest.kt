package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintCleanerConditionsTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintCleanerConditions::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintCleanerConditionsConfig.yaml")
) {
    @Test
    fun testTaintBadArg() {
        checkWithException(
            TaintCleanerConditions::badArg,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadReturn() {
        checkWithException(
            TaintCleanerConditions::badReturn,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintCleanerConditions::badThis,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodArg() {
        checkWithException(
            TaintCleanerConditions::goodArg,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodReturn() {
        checkWithException(
            TaintCleanerConditions::goodReturn,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodThis() {
        checkWithException(
            TaintCleanerConditions::goodThis,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
