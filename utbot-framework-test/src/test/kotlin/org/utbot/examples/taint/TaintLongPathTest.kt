package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
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
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGood() {
        checkWithException(
            TaintLongPath::good,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
