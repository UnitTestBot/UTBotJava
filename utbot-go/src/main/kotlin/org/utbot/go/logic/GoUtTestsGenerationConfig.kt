package org.utbot.go.logic

import java.nio.file.Path

class GoUtTestsGenerationConfig(
    val goExecutableAbsolutePath: Path,
    val gopathAbsolutePath: Path,
    val eachFunctionExecutionTimeoutMillis: Long,
    val allFunctionExecutionTimeoutMillis: Long
) {

    companion object Constants {
        const val DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS: Long = 60000
        const val DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS: Long = 1000
    }
}