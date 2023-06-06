package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintPassSimpleTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintPassSimple::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintPassSimpleConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            TaintPassSimple::bad,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadDoublePass() {
        checkWithException(
            TaintPassSimple::badDoublePass,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGood() {
        check(
            TaintPassSimple::good,
            eq(1), // only success
        )
    }
}
