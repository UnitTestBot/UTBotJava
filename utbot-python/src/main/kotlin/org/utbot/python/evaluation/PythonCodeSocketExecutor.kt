package org.utbot.python.evaluation

import org.utbot.fuzzer.FuzzedValue
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod


data class ExecutionDescription(
    val functionName: String,
    val imports: List<String>,
    val argumentsIds: List<String>,
    val serializedMemory: String
)


class PythonCodeSocketExecutor(
    override val method: PythonMethod,
    override val methodArguments: FunctionArguments,
    override val moduleToImport: String,
    override val pythonPath: String,
    override val syspathDirectories: Set<String>,
    override val executionTimeout: Long,
) : PythonCodeExecutor {
    val executionClient = ExecutionClient("localhost", 12011)  // TODO: create port manager
    override fun run(
        fuzzedValues: List<FuzzedValue>,
        additionalModulesToImport: Set<String>): PythonEvaluationResult {
//        val input =
        TODO()
    }
}