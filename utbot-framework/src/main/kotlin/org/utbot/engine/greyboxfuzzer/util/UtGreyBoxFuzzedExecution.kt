package org.utbot.engine.greyboxfuzzer.util

import org.utbot.framework.plugin.api.*

class UtGreyBoxFuzzedExecution(
    stateBefore: EnvironmentModels,
    result: UtExecutionResult,
    stateAfter: EnvironmentModels = stateBefore,
    coverage: Coverage? = null,
    summary: List<DocStatement>? = null,
    testMethodName: String? = null,
    displayName: String? = null
): UtExecution(stateBefore, stateAfter, result, coverage, summary, testMethodName, displayName) {

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