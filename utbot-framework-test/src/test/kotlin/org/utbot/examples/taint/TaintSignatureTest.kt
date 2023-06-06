package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintSignatureTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintSignature::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintSignatureConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            TaintSignature::badFakeCleaner,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGoodCleaner() {
        check(
            TaintSignature::goodCleaner,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodFakeSources() {
        check(
            TaintSignature::goodFakeSources,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodFakePass() {
        check(
            TaintSignature::goodFakePass,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodFakeSink() {
        check(
            TaintSignature::goodFakeSink,
            eq(1), // only success
        )
    }
}
