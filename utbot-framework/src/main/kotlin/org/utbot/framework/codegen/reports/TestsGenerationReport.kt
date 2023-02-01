package org.utbot.framework.codegen.reports

import org.utbot.common.appendHtmlLine
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodType
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.util.kClass
import kotlin.reflect.KClass

typealias MethodGeneratedTests = MutableMap<ExecutableId, MutableSet<CgTestMethod>>
typealias ErrorsCount = Map<String, Int>

data class TestsGenerationReport(
    val executables: MutableSet<ExecutableId> = mutableSetOf(),
    var successfulExecutions: MethodGeneratedTests = mutableMapOf(),
    var timeoutExecutions: MethodGeneratedTests = mutableMapOf(),
    var failedExecutions: MethodGeneratedTests = mutableMapOf(),
    var artificiallyFailedExecutions: MethodGeneratedTests = mutableMapOf(),
    var crashExecutions: MethodGeneratedTests = mutableMapOf(),
    var errors: MutableMap<ExecutableId, ErrorsCount> = mutableMapOf()
) {
    val classUnderTest: KClass<*>
        get() = executables.firstOrNull()?.classId?.kClass
            ?: error("No executables found in test report")

    val initialWarnings: MutableList<() -> String> = mutableListOf()
    val hasWarnings: Boolean
        get() = initialWarnings.isNotEmpty()

    val detailedStatistics: String
        get() = buildString {
            appendHtmlLine("Class: ${classUnderTest.qualifiedName}")
            val testMethodsStatistic = executables.map { it.countTestMethods() }
            val errors = executables.map { it.countErrors() }
            val overallErrors = errors.sum()

            appendHtmlLine("Successful test methods: ${testMethodsStatistic.sumOf { it.successful }}")
            appendHtmlLine(
                "Failing because of unexpected exception test methods: ${testMethodsStatistic.sumOf { it.failing }}"
            )
            appendHtmlLine(
                "Failing because of exception (according to custom settings) test methods: " +
                        "${testMethodsStatistic.sumOf { it.artificiallyFailing }}"
            )
            appendHtmlLine(
                "Failing because of exceeding timeout test methods: ${testMethodsStatistic.sumOf { it.timeout }}"
            )
            appendHtmlLine(
                "Failing because of possible JVM crash test methods: ${testMethodsStatistic.sumOf { it.crashes }}"
            )
            appendHtmlLine("Not generated because of internal errors test methods: $overallErrors")
        }

    fun addMethodErrors(testSet: CgMethodTestSet, errors: Map<String, Int>) {
        this.executables += testSet.executableId
        this.errors[testSet.executableId] = errors
    }

    fun addTestsByType(testSet: CgMethodTestSet, testMethods: List<CgTestMethod>) {
        with(testSet.executableId) {
            executables += this

            testMethods.forEach {
                when (it.type) {
                    CgTestMethodType.SUCCESSFUL, CgTestMethodType.PASSED_EXCEPTION -> updateExecutions(it, successfulExecutions)
                    CgTestMethodType.FAILING -> updateExecutions(it, failedExecutions)
                    CgTestMethodType.ARTIFICIAL -> updateExecutions(it, artificiallyFailedExecutions)
                    CgTestMethodType.TIMEOUT -> updateExecutions(it, timeoutExecutions)
                    CgTestMethodType.CRASH -> updateExecutions(it, crashExecutions)
                    CgTestMethodType.PARAMETRIZED -> {
                        // Parametrized tests are not supported in the tests report yet
                        // TODO JIRA:1507
                    }
                }
            }
        }
    }

    fun countTestMethods() = executables.map { it.countTestMethods() }.sumOf { it.count }

    fun toString(isShort: Boolean): String = buildString {
        appendHtmlLine("Target: ${classUnderTest.qualifiedName}")
        if (initialWarnings.isNotEmpty()) {
            initialWarnings.forEach { appendHtmlLine(it()) }
            appendHtmlLine()
        }

        appendHtmlLine("Overall test methods: ${countTestMethods()}")

        if (!isShort) {
            appendHtmlLine(detailedStatistics)
        }
    }

    override fun toString(): String = toString(false)

    private fun ExecutableId.countTestMethods(): TestMethodStatistic = TestMethodStatistic(
        testMethodsNumber(successfulExecutions),
        testMethodsNumber(failedExecutions),
        testMethodsNumber(artificiallyFailedExecutions),
        testMethodsNumber(timeoutExecutions),
        testMethodsNumber(crashExecutions),
    )

    private fun ExecutableId.countErrors(): Int = errors.getOrDefault(this, emptyMap()).values.sum()

    private fun ExecutableId.testMethodsNumber(executables: MethodGeneratedTests): Int =
        executables.getOrDefault(this, emptySet()).size

    private fun ExecutableId.updateExecutions(it: CgTestMethod, executions: MethodGeneratedTests) {
        executions.getOrPut(this) { mutableSetOf() } += it
    }

    private data class TestMethodStatistic(
        val successful: Int,
        val failing: Int,
        val artificiallyFailing: Int,
        val timeout: Int,
        val crashes: Int,
    ) {
        val count: Int = successful + failing + artificiallyFailing + timeout + crashes
    }
}