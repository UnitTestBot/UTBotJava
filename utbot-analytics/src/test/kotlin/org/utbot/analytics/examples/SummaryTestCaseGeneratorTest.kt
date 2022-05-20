package org.utbot.analytics.examples

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.CoverageMatcher
import org.utbot.examples.DoNotCalculate
import org.utbot.framework.UtSettings.checkNpeForFinalFields
import org.utbot.framework.UtSettings.checkNpeInNestedMethods
import org.utbot.framework.UtSettings.checkNpeInNestedNotPrivateMethods
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.codegen.TestExecution
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.summary.summarize
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag

@Tag("Summary")
open class SummaryTestCaseGeneratorTest(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = true,
    languagePipelines: List<CodeGenerationLanguageLastStage> = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, TestExecution)
    )
) : AbstractTestCaseGeneratorTest(testClass, testCodeGeneration, languagePipelines) {
    private lateinit var cookie: AutoCloseable

    @BeforeEach
    fun setup() {
        cookie = UtContext.setUtContext(UtContext(ClassLoader.getSystemClassLoader()))
    }

    @AfterEach
    fun tearDown() {
        cookie.close()
    }

    protected inline fun <reified R> checkNoArguments(
        method: KFunction1<*, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, displayNames)

    protected inline fun <reified T, reified R> checkOneArgument(
        method: KFunction2<*, T, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, displayNames)

    protected inline fun <reified T1, reified T2, reified R> checkTwoArguments(
        method: KFunction3<*, T1, T2, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, displayNames)

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkThreeArguments(
        method: KFunction4<*, T1, T2, T3, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, displayNames)

    inline fun <reified R> check(
        method: KFunction<R>,
        mockStrategy: MockStrategyApi,
        coverageMatcher: CoverageMatcher,
        summaryKeys: List<String>,
        displayNames: List<String>
    ) {
        workaround(WorkaroundReason.HACK) {
            // @todo change to the constructor parameter
            checkSolverTimeoutMillis = 0
            checkNpeInNestedMethods = true
            checkNpeInNestedNotPrivateMethods = true
            checkNpeForFinalFields = true
        }
        val utMethod = UtMethod.from(method)
        val testCase = executionsModel(utMethod, mockStrategy)
        testCase.summarize(searchDirectory)
        testCase.executions.checkMatchersWithTextSummary(summaryKeys)
        testCase.executions.checkMatchersWithDisplayNames(displayNames)
    }

    fun List<UtExecution>.checkMatchersWithTextSummary(
        summaryTextKeys: List<String>,
    ) {
        if (summaryTextKeys.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            summaryTextKeys.none { summaryKey -> execution.summary?.contains(summaryKey) == true }
        }
        Assertions.assertTrue(notMatchedExecutions.isEmpty()) { "Not matched summaries ${summaries(notMatchedExecutions)}" }
    }

    fun List<UtExecution>.checkMatchersWithDisplayNames(
        displayNames: List<String>,
    ) {
        if (displayNames.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            displayNames.none { displayName -> execution.displayName?.equals(displayName) == true }
        }
        Assertions.assertTrue(notMatchedExecutions.isEmpty()) { "Not matched summaries ${summaries(notMatchedExecutions)}" }
    }

    private fun summaries(executions: List<UtExecution>): String {
        var result = "";
        executions.forEach {
            result += it.summary?.joinToString(separator = "", postfix = "\n")
        }
        return result
    }

}