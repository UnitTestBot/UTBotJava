package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintLongPathTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintLongPath::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintLongPathConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            TaintLongPath::bad,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() }
        )
    }

    @Test
    fun testTaintGood() {
        check(
            TaintLongPath::good,
            eq(1), // only success
        )
    }
}
