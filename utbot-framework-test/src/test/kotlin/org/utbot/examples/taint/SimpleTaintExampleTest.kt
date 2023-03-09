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

internal class SimpleTaintExampleTest : UtValueTestCaseCheckerForTaint(
    testClass = SimpleTaintExample::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    ),
    taintConfigurationProvider = TaintConfigurationProviderResources("taint/SimpleExampleConfig.yaml")
) {
    @Test
    fun testTaintBad() {
        checkWithException(
            SimpleTaintExample::bad,
            ge(2), // success & taint error
            { r -> r.isException<TaintAnalysisError>() },
        )
    }

    @Test
    fun testTaintGood() {
        check(
            SimpleTaintExample::good,
            eq(1), // only success
        )
    }
}
