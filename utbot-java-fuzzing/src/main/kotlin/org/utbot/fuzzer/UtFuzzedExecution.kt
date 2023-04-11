package org.utbot.fuzzer

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult

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
    val fuzzedMethodDescription:  FuzzedMethodDescription? = null
) : UtExecution(stateBefore, stateAfter, result, coverage, summary, testMethodName, displayName) {
    /**
     * By design the 'before' and 'after' states contain info about the same fields.
     * It means that it is not possible for a field to be present at 'before' and to be absent at 'after'.
     * The reverse is also impossible.
     */
    val staticFields: Set<FieldId>
        get() = stateBefore.statics.keys // TODO: should we keep it for the Fuzzed Execution?

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
}