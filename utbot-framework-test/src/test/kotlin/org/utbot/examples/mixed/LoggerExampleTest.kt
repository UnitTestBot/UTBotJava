package org.utbot.examples.mixed

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.isNull
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class LoggerExampleTest : UtValueTestCaseChecker(
    testClass = LoggerExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testExample() {
        checkMocksAndInstrumentation(
            LoggerExample::example,
            eq(2),
            { _, instrumentation, _ -> theOnlyStaticMockValue(instrumentation).isNull() },
            { mocks, instrumentation, r -> mocks.size == 3 && instrumentation.size == 1 && r == 15 },
            additionalDependencies = arrayOf(org.slf4j.Logger::class.java),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLoggerUsage() {
        checkMocksAndInstrumentation(
            LoggerExample::loggerUsage,
            eq(3),
            { _, instrumentation, _ -> theOnlyStaticMockValue(instrumentation).isNull() },
            { mocks, instrumentation, r ->
                (mocks.single().values.single() as UtConcreteValue<*>).value == false && instrumentation.size == 1 && r == 2
            },
            { mocks, instrumentation, r ->
                (mocks.single().values.single() as UtConcreteValue<*>).value == true && instrumentation.size == 1 && r == 1
            },
            additionalDependencies = arrayOf(org.slf4j.Logger::class.java),
            coverage = DoNotCalculate
        )
    }

    private fun theOnlyStaticMockValue(instrumentation: List<UtInstrumentation>): UtModel =
        instrumentation
            .filterIsInstance<UtStaticMethodInstrumentation>()
            .single()
            .values
            .single()
}