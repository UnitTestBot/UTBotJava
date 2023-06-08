package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintCleanerSimpleTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintCleanerSimple::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintCleanerSimpleConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            TaintCleanerSimple::bad,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGood() {
        checkWithException(
            TaintCleanerSimple::good,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
