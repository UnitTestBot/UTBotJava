package org.utbot.go.logic

import java.nio.file.Path

enum class TestsGenerationMode {
    DEFAULT, FUZZING_MODE
}

class GoUtTestsGenerationConfig(
    val goExecutableAbsolutePath: Path,
    val gopathAbsolutePath: Path,
    val numberOfFuzzingProcess: Int,
    val mode: TestsGenerationMode,
    val eachFunctionExecutionTimeoutMillis: Long,
    val allFunctionExecutionTimeoutMillis: Long
) {

    companion object Constants {
        const val DEFAULT_NUMBER_OF_FUZZING_PROCESSES: Int = 8
        const val DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS: Long = 60000
        const val DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS: Long = 1000
    }
}