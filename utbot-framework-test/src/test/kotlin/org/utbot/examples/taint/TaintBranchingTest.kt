package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintBranchingTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintBranching::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintBranchingConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            TaintBranching::bad,
            eq(3), // success (x2) & taint error
            { cond, r -> cond == r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGood() {
        checkWithException(
            TaintBranching::good,
            eq(2), // success in both cases
            { _, r -> r.isSuccess },
        )
    }
}
