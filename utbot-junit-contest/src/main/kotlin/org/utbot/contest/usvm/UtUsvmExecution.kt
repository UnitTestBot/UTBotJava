package org.utbot.contest.usvm

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
    override val instrumentation: List<UtInstrumentation>
) : UtExecution(
    stateBefore,
    stateAfter,
    result,
    coverage,
    summary,
    testMethodName,
    displayName
), UtExecutionWithInstrumentation {
    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?
    ): UtExecution = UtUsvmExecution(
        stateBefore,
        stateAfter,
        result,
        coverage,
        summary,
        testMethodName,
        displayName,
        instrumentation
    )
}