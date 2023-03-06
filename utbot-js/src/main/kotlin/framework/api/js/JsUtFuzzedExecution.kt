package framework.api.js

import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult

class JsUtFuzzedExecution(
    stateBefore: EnvironmentModels,
    stateAfter: EnvironmentModels,
    result: UtExecutionResult
) : UtExecution(stateBefore, stateAfter, result, null, null, null, null)
