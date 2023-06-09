package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
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
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadReturn() {
        checkWithException(
            TaintPassConditions::badReturn,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadThis() {
        checkWithException(
            TaintPassConditions::badThis,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodArg() {
        checkWithException(
            TaintPassConditions::goodArg,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodReturn() {
        checkWithException(
            TaintPassConditions::goodReturn,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodThis() {
        checkWithException(
            TaintPassConditions::goodThis,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
