package org.utbot.go.executor

internal data class RawPanicMessage(val rawValue: String?, val goTypeName: String, val implementsError: Boolean)

internal data class RawExecutionResult(
    val functionName: String,
    val resultRawValues: List<String?>,
    val panicMessage: RawPanicMessage?
)

internal data class RawExecutionResults(val results: List<RawExecutionResult>)