package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintSourceConditionsTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintSourceConditions::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintSourceConditionsConfig.yaml")
) {
    @Test
    fun testTaintBadArg() {
        checkWithException(
            TaintSourceConditions::badArg,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadReturn() {
        checkWithException(
            TaintSourceConditions::badReturn,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintSourceConditions::badThis,
            ge(1), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGoodArg() {
        check(
            TaintSourceConditions::goodArg,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodReturn() {
        check(
            TaintSourceConditions::goodReturn,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodThis() {
        check(
            TaintSourceConditions::goodThis,
            eq(1), // only success
        )
    }
}
