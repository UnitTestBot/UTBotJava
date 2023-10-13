package framework.api.js

import org.utbot.framework.plugin.api.*

class JsUtFuzzedExecution(
    stateBefore: EnvironmentModels,
    stateAfter: EnvironmentModels,
    result: UtExecutionResult
) : UtExecution(stateBefore, stateAfter, result, null, null, null, null) {
    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?
    ): UtExecution {
        return JsUtFuzzedExecution(
            stateBefore = stateBefore,
            stateAfter = stateAfter,
            result = result
        )
    }
}
