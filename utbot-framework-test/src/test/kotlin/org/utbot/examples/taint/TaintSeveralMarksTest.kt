package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class TaintSeveralMarksTest : UtValueTestCaseCheckerForTaint(
    testClass = TaintSeveralMarks::class,
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/TaintSeveralMarksConfig.yaml")
) {
    @Test
    fun testTaintBad1() {
        checkWithException(
            TaintSeveralMarks::bad1,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBad2() {
        checkWithException(
            TaintSeveralMarks::bad2,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadSourceAll() {
        checkWithException(
            TaintSeveralMarks::badSourceAll,
            eq(4), // success & taint error (x3)
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadSinkAll() {
        checkWithException(
            TaintSeveralMarks::badSinkAll,
            eq(3), // success & taint error (x2)
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintBadWrongCleaner() {
        checkWithException(
            TaintSeveralMarks::badWrongCleaner,
            eq(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGood1() {
        checkWithException(
            TaintSeveralMarks::good1,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGood2() {
        checkWithException(
            TaintSeveralMarks::good2,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodWrongSource() {
        checkWithException(
            TaintSeveralMarks::goodWrongSource,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }

    @Test
    fun testTaintGoodWrongSink() {
        checkWithException(
            TaintSeveralMarks::goodWrongSink,
            eq(1), // only success
            { r -> r.isSuccess }
        )
    }

    @Test
    fun testTaintGoodWrongPass() {
        checkWithException(
            TaintSeveralMarks::goodWrongPass,
            eq(1), // only success
            { r -> r.isSuccess },
        )
    }
}
