package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
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
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodCleaner() {
        checkWithException(
            TaintSignature::goodCleaner,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodFakeSources() {
        checkWithException(
            TaintSignature::goodFakeSources,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodFakePass() {
        checkWithException(
            TaintSignature::goodFakePass,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodFakeSink() {
        checkWithException(
            TaintSignature::goodFakeSink,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
