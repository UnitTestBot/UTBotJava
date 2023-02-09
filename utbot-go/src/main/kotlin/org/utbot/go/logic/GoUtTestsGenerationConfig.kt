package org.utbot.go.logic

import org.utbot.go.api.GoUtFunction

data class EachExecutionTimeoutsMillisConfig(private val eachFunctionExecutionTimeoutMillis: Long) {
    @Suppress("UNUSED_PARAMETER") // TODO: support finer tuning
    operator fun get(function: GoUtFunction): Long = eachFunctionExecutionTimeoutMillis
}

class GoUtTestsGenerationConfig(
    val goExecutableAbsolutePath: String,
    eachFunctionExecutionTimeoutMillis: Long,
    allFunctionExecutionTimeoutMillis: Long
) {

    companion object Constants {
        const val DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS: Long = 60000
        const val DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS: Long = 1000
    }

    val eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig =
        EachExecutionTimeoutsMillisConfig(eachFunctionExecutionTimeoutMillis)

    val allExecutionTimeoutsMillisConfig: Long = allFunctionExecutionTimeoutMillis
}