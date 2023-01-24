package org.utbot.go.api

import org.utbot.go.framework.api.go.GoUtModel

interface GoUtExecutionResult {
    val trace: List<Int>
}

interface GoUtExecutionCompleted : GoUtExecutionResult {
    val models: List<GoUtModel>
}

data class GoUtExecutionSuccess(
    override val models: List<GoUtModel>,
    override val trace: List<Int>
) : GoUtExecutionCompleted

data class GoUtExecutionWithNonNilError(
    override val models: List<GoUtModel>,
    override val trace: List<Int>
) : GoUtExecutionCompleted

data class GoUtPanicFailure(
    val panicValue: GoUtModel,
    val panicValueIsErrorMessage: Boolean,
    override val trace: List<Int>
) : GoUtExecutionResult

data class GoUtTimeoutExceeded(
    val timeoutMillis: Long,
    override val trace: List<Int>
) : GoUtExecutionResult