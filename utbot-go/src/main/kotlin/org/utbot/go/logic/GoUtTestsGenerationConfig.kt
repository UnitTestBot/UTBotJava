package org.utbot.go.logic

class GoUtTestsGenerationConfig(
    val goExecutableAbsolutePath: String,
    val eachFunctionExecutionTimeoutMillis: Long,
    val allFunctionExecutionTimeoutMillis: Long
) {

    companion object Constants {
        const val DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS: Long = 60000
        const val DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS: Long = 1000
    }
}