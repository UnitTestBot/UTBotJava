package org.utbot.usvm.converter

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionWithInstrumentation
import org.utbot.framework.plugin.api.UtInstrumentation

class UtUsvmExecution(
    stateBefore: EnvironmentModels,
    stateAfter: EnvironmentModels,
    result: UtExecutionResult,
    coverage: Coverage?,
    summary: List<DocStatement>? = emptyList(),
    testMethodName: String? = null,
    displayName: String? = null,
    instrumentation: List<UtInstrumentation>
) : UtExecutionWithInstrumentation(
    stateBefore,
    stateAfter,
    result,
    coverage,
    summary,
    testMethodName,
    displayName,
    instrumentation,
) {
    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?,
        instrumentation: List<UtInstrumentation>,
    ): UtExecution = UtUsvmExecution(
        stateBefore,
        stateAfter,
        result,
        coverage,
        summary,
        testMethodName,
        displayName,
        instrumentation,
    )
}
