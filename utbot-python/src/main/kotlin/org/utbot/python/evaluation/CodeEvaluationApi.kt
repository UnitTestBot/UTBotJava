package org.utbot.python.evaluation

import org.utbot.framework.plugin.api.Coverage
import org.utbot.fuzzer.FuzzedValue
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.evaluation.serialiation.MemoryDump

interface PythonCodeExecutor {
    val method: PythonMethod
    val methodArguments: FunctionArguments
    val moduleToImport: String
    val pythonPath: String
    val syspathDirectories: Set<String>
    val executionTimeout: Long

    fun run(
        fuzzedValues: List<FuzzedValue>,
        additionalModulesToImport: Set<String>
    ): PythonEvaluationResult
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
    val coverage: Coverage,
    val stateBefore: MemoryDump,
    val stateAfter: MemoryDump,
    val modelListIds: List<String>,
    val resultId: String,
) : PythonEvaluationResult()
