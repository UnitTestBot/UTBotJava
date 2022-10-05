package org.utbot.go.api

import org.utbot.framework.plugin.api.go.GoUtModel

interface GoUtExecutionResult

interface GoUtExecutionCompleted : GoUtExecutionResult {
    val models: List<GoUtModel>
}

data class GoUtExecutionSuccess(override val models: List<GoUtModel>) : GoUtExecutionCompleted

data class GoUtExecutionWithNonNilError(override val models: List<GoUtModel>) : GoUtExecutionCompleted

data class GoUtPanicFailure(
    val panicValue: GoUtModel,
    val panicValueSourceGoType: GoTypeId
) : GoUtExecutionResult