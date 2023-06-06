package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintSinkConditionsTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintSinkConditions::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintSinkConditionsConfig.yaml")
) {
    @Test
    fun testTaintBadArg() {
        checkWithException(
            TaintSinkConditions::badArg,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintSinkConditions::badThis,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGoodArg() {
        check(
            TaintSinkConditions::goodArg,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodThis() {
        check(
            TaintSinkConditions::goodThis,
            eq(1), // only success
        )
    }
}
