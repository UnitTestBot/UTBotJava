package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
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
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadReturn() {
        checkWithException(
            TaintCleanerConditions::badReturn,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintCleanerConditions::badThis,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGoodArg() {
        check(
            TaintCleanerConditions::goodArg,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodReturn() {
        check(
            TaintCleanerConditions::goodReturn,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodThis() {
        check(
            TaintCleanerConditions::goodThis,
            eq(1), // only success
        )
    }
}
