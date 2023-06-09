package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
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
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadReturn() {
        checkWithException(
            TaintSourceConditions::badReturn,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintSourceConditions::badThis,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodArg() {
        checkWithException(
            TaintSourceConditions::goodArg,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodReturn() {
        checkWithException(
            TaintSourceConditions::goodReturn,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodThis() {
        checkWithException(
            TaintSourceConditions::goodThis,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
