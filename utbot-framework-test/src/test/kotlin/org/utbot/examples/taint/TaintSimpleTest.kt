package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintSimpleTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintSimple::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintSimpleConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            TaintSimple::bad,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGood() {
        checkWithException(
            TaintSimple::good,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
