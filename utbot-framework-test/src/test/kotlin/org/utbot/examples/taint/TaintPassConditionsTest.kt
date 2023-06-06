package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintPassConditionsTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintPassConditions::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintPassConditionsConfig.yaml")
) {
    @Test
    fun testTaintBadArg() {
        checkWithException(
            TaintPassConditions::badArg,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadReturn() {
        checkWithException(
            TaintPassConditions::badReturn,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintPassConditions::badThis,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGoodArg() {
        check(
            TaintPassConditions::goodArg,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodReturn() {
        check(
            TaintPassConditions::goodReturn,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodThis() {
        check(
            TaintPassConditions::goodThis,
            eq(1), // only success
        )
    }
}
