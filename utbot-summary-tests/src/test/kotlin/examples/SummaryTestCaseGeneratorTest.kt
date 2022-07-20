package examples

import org.junit.jupiter.api.*
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.CoverageMatcher
import org.utbot.examples.DoNotCalculate
import org.utbot.framework.UtSettings.checkNpeInNestedMethods
import org.utbot.framework.UtSettings.checkNpeInNestedNotPrivateMethods
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.codegen.TestExecution
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.summary.comment.nextSynonyms
import org.utbot.summary.summarize
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4


@Disabled
open class SummaryTestCaseGeneratorTest(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = false,
    languagePipelines: List<CodeGenerationLanguageLastStage> = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, TestExecution)
    )
) : UtValueTestCaseChecker(testClass, testCodeGeneration, languagePipelines) {
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
        methodNames: List<String> = listOf(),
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)

    protected inline fun <reified T, reified R> checkOneArgument(
        method: KFunction2<*, T, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        methodNames: List<String> = listOf(),
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)

    protected inline fun <reified T1, reified T2, reified R> checkTwoArguments(
        method: KFunction3<*, T1, T2, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        methodNames: List<String> = listOf(),
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkThreeArguments(
        method: KFunction4<*, T1, T2, T3, R>,
        coverage: CoverageMatcher = DoNotCalculate,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        summaryKeys: List<String>,
        methodNames: List<String> = listOf(),
        displayNames: List<String> = listOf()
    ) = check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)

    inline fun <reified R> check(
        method: KFunction<R>,
        mockStrategy: MockStrategyApi,
        coverageMatcher: CoverageMatcher,
        summaryKeys: List<String>,
        methodNames: List<String>,
        displayNames: List<String>
    ) {
        workaround(WorkaroundReason.HACK) {
            // @todo change to the constructor parameter
            checkSolverTimeoutMillis = 0
            checkNpeInNestedMethods = true
            checkNpeInNestedNotPrivateMethods = true
        }
        val utMethod = UtMethod.from(method)
        val testSet = executionsModel(utMethod, mockStrategy)
        testSet.summarize(searchDirectory)

        testSet.executions.checkMatchersWithTextSummary(summaryKeys)
        testSet.executions.checkMatchersWithMethodNames(methodNames)
        testSet.executions.checkMatchersWithDisplayNames(displayNames)
    }

    /**
     * It removes from the String all whitespaces, tabs etc.
     *
     * Also, it replaces all randomly added words from [nextSynonyms] that totally influence on the determinism in test name generation.
     *
     * @see <a href="https://www.baeldung.com/java-regex-s-splus">Explanation of the used regular expression.</a>
     */
    private fun String.normalize(): String {
        var result = this.replace("\\s+".toRegex(), "")
        nextSynonyms.forEach {
            result = result.replace(it, "")
        }
        return result
    }


    fun List<UtExecution>.checkMatchersWithTextSummary(
        summaryTextKeys: List<String>,
    ) {
        if (summaryTextKeys.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            summaryTextKeys.none { summaryKey -> val normalize = execution.summary?.toString()?.normalize()
                normalize?.contains(summaryKey.normalize()) == true }
        }
        Assertions.assertTrue(notMatchedExecutions.isEmpty()) { "Not matched comments ${summaries(notMatchedExecutions)}" }
    }

    fun List<UtExecution>.checkMatchersWithMethodNames(
        methodNames: List<String>,
    ) {
        if (methodNames.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            methodNames.none { methodName -> execution.testMethodName?.equals(methodName) == true }
        }
        Assertions.assertTrue(notMatchedExecutions.isEmpty()) { "Not matched test names ${summaries(notMatchedExecutions)}" }
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
        Assertions.assertTrue(notMatchedExecutions.isEmpty()) { "Not matched display names ${summaries(notMatchedExecutions)}" }
    }

    private fun summaries(executions: List<UtExecution>): String {
        var result = ""
        executions.forEach {
            result += it.summary?.joinToString(separator = "", postfix = "\n")
        }
        return result
    }
}