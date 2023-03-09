package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.TaintAnalysisError
import org.utbot.taint.TaintConfigurationProviderResources
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.CodeGeneration
import org.utbot.testing.UtValueTestCaseCheckerForTaint
import org.utbot.testing.isException

internal class LongTaintPathTest : UtValueTestCaseCheckerForTaint(
    testClass = LongTaintPath::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    ),
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/LongTaintPathConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            LongTaintPath::bad,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() }
        )
    }

    @Test
    fun testTaintGood() {
        check(
            LongTaintPath::good,
            eq(1), // only success
        )
    }
}
