package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
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
            ge(2), // success & taint error
            { cond, r -> cond == r.isException<TaintAnalysisError>() }
        )
    }

    @Test
    fun testTaintGood() {
        check(
            TaintBranching::good,
            eq(2), // success in both cases
        )
    }
}
