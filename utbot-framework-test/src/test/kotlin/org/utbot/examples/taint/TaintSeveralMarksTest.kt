package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
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
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBad2() {
        checkWithException(
            TaintSeveralMarks::bad2,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadAll() {
        checkWithException(
            TaintSeveralMarks::badAll,
            ge(3), // success & taint error (x2)
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintBadWrongCleaner() {
        checkWithException(
            TaintSeveralMarks::badWrongCleaner,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGood1() {
        check(
            TaintSeveralMarks::good1,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGood2() {
        check(
            TaintSeveralMarks::good2,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodWrongSource() {
        check(
            TaintSeveralMarks::goodWrongSource,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodWrongSink() {
        check(
            TaintSeveralMarks::goodWrongSink,
            eq(1), // only success
        )
    }

    @Test
    fun testTaintGoodWrongPass() {
        check(
            TaintSeveralMarks::goodWrongPass,
            eq(1), // only success
        )
    }
}
