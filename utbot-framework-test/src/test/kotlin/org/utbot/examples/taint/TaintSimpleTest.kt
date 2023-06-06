package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
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
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGood() {
        check(
            TaintSimple::good,
            eq(1), // only success
        )
    }
}
