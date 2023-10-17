package org.utbot.python.evaluation

import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.evaluation.serialization.MemoryDump
import org.utbot.python.evaluation.coverage.PyCoverage

interface PythonCodeExecutor {
    val method: PythonMethod
    val moduleToImport: String
    val pythonPath: String
    val syspathDirectories: Set<String>
    val executionTimeout: Long

    fun run(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>
    ): PythonEvaluationResult

    fun runWithCoverage(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>,
        coverageId: String,
    ): PythonEvaluationResult

    fun stop()
}

sealed class PythonEvaluationResult

data class PythonEvaluationError(
    val status: Int,
    val message: String,
    val stackTrace: List<String>
) : PythonEvaluationResult()

data class PythonEvaluationTimeout(
    val message: String = "Timeout"
) : PythonEvaluationResult()

data class PythonEvaluationSuccess(
    val isException: Boolean,
    val coverage: PyCoverage,
    val stateInit: MemoryDump,
    val stateBefore: MemoryDump,
    val stateAfter: MemoryDump,
    val diffIds: List<Long>,
    val modelListIds: List<String>,
    val resultId: String,
) : PythonEvaluationResult()
