package org.utbot.fuzzer

import org.utbot.framework.plugin.api.*

/**
 * Fuzzed execution.
 *
 * Contains:
 * - execution parameters, including thisInstance;
 * - result;
 * - static fields changed during execution;
 * - coverage information (instructions) if this execution was obtained from the concrete execution.
 * - comments, method names and display names created by utbot-summary module.
 */
class UtFuzzedExecution(
    stateBefore: EnvironmentModels,
    stateAfter: EnvironmentModels,
    result: UtExecutionResult,
    coverage: Coverage? = null,
    summary: List<DocStatement>? = null,
    testMethodName: String? = null,
    displayName: String? = null,
    val fuzzingValues:  List<FuzzedValue>? = null,
    val fuzzedMethodDescription:  FuzzedMethodDescription? = null,
    override val instrumentation: List<UtInstrumentation> = emptyList(),
) : UtExecution(stateBefore, stateAfter, result, coverage, summary, testMethodName, displayName), UtExecutionWithInstrumentation {
    /**
     * By design the 'before' and 'after' states contain info about the same fields.
     * It means that it is not possible for a field to be present at 'before' and to be absent at 'after'.
     * The reverse is also impossible.
     */
    val staticFields: Set<FieldId>
        get() = stateBefore.statics.keys // TODO: should we keep it for the Fuzzed Execution?

    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?
    ): UtExecution {
        return UtFuzzedExecution(
            stateBefore,
            stateAfter,
            result,
            coverage,
            summary,
            testMethodName,
            displayName,
            fuzzingValues = fuzzingValues,
            fuzzedMethodDescription = fuzzedMethodDescription,
            instrumentation = instrumentation,
        )
    }

    override fun toString(): String = buildString {
        append("UtFuzzedExecution(")
        appendLine()

        append("<State before>:")
        appendLine()
        append(stateBefore)
        appendLine()

        append("<State after>:")
        appendLine()
        append(stateAfter)
        appendLine()

        append("<Result>:")
        appendLine()
        append(result)
        append(")")
    }

    fun copy(
        stateBefore: EnvironmentModels = this.stateBefore,
        stateAfter: EnvironmentModels = this.stateAfter,
        result: UtExecutionResult = this.result,
        coverage: Coverage? = this.coverage,
        summary: List<DocStatement>? = this.summary,
        testMethodName: String? = this.testMethodName,
        displayName: String? = this.displayName,
        fuzzingValues: List<FuzzedValue>? = this.fuzzingValues,
        fuzzedMethodDescription: FuzzedMethodDescription? = this.fuzzedMethodDescription,
        instrumentation: List<UtInstrumentation> = this.instrumentation,
    ): UtExecution = UtFuzzedExecution(
        stateBefore = stateBefore,
        stateAfter = stateAfter,
        result = result,
        coverage = coverage,
        summary = summary,
        testMethodName = testMethodName,
        displayName = displayName,
        fuzzingValues = fuzzingValues,
        fuzzedMethodDescription = fuzzedMethodDescription,
        instrumentation = instrumentation,
    )
}