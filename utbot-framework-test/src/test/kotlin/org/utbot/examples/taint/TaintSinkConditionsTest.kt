package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
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
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintSinkConditions::badThis,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodArg() {
        checkWithException(
            TaintSinkConditions::goodArg,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodThis() {
        checkWithException(
            TaintSinkConditions::goodThis,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
