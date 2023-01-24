package org.utbot.greyboxfuzzer.util

import org.utbot.framework.plugin.api.*
import org.utbot.instrumentation.instrumentation.execution.UtFuzzingConcreteExecutionResult

//class UtFuzzExecutionResult() : UtConcreteExecutionResult

class UtGreyBoxFuzzedExecution(
    stateBefore: EnvironmentModels,
    val fuzzingResult: UtFuzzingConcreteExecutionResult,
    stateAfter: EnvironmentModels = stateBefore,
    coverage: Coverage? = null,
    summary: List<DocStatement>? = null,
    testMethodName: String? = null,
    displayName: String? = null
): UtExecution(stateBefore, stateAfter, fuzzingResult.result, coverage, summary, testMethodName, displayName) {

    override fun toString(): String = buildString {
        append("UtGreyBoxFuzzedExecution(")
        appendLine()

        append("<State before>:")
        appendLine()
        append(stateBefore)
        appendLine()

        append("<Result>:")
        appendLine()
        append(result)
        append(")")
    }


}