package org.utbot.python.evaluation

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.evaluation.serialiation.ExecutionRequest
import org.utbot.python.evaluation.serialiation.ExecutionRequestSerializer
import org.utbot.python.evaluation.serialiation.ExecutionResultDeserializer
import org.utbot.python.evaluation.serialiation.FailExecution
import org.utbot.python.evaluation.serialiation.PythonExecutionResult
import org.utbot.python.evaluation.serialiation.SuccessExecution
import org.utbot.python.evaluation.serialiation.serializeObjects
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.utils.TemporaryFileManager


class PythonCodeSocketExecutor(
    override val method: PythonMethod,
    override val moduleToImport: String,
    override val pythonPath: String,
    override val syspathDirectories: Set<String>,
    override val executionTimeout: Long,
) : PythonCodeExecutor {
    private val executionClient = ExecutionClient("localhost", 12011, pythonPath)  // TODO: create port manager
    override fun run(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>
    ): PythonEvaluationResult {
        val (arguments, memory) = serializeObjects(fuzzedValues.allArguments.map { it.tree })
        val coverageDatabasePath = TemporaryFileManager.assignTemporaryFile(
            tag = "coverage_db_" + method.name,
            addToCleaner = false,
        )
        val request = ExecutionRequest(
            method.name,
            (additionalModulesToImport + moduleToImport).toList(),
            syspathDirectories.toList(),
            arguments,
            memory,
            coverageDatabasePath.path,
            method.moduleFilename,
        )
        val message = ExecutionRequestSerializer.serializeRequest(request) ?: error("Cannot serialize request to python executor")
        executionClient.sendMessage(message)
        val response = executionClient.receiveMessage() ?: error("Cannot read response from python executor")
        val executionResult = ExecutionResultDeserializer.parseExecutionResult(response) ?: error("Cannot parse execution result")
        return parseExecutionResult(executionResult)
    }

    private fun parseExecutionResult(executionResult: PythonExecutionResult): PythonEvaluationResult {
        val parsingException = PythonEvaluationError(
            0,
            "Incorrect format of output",
            emptyList()
        )
        return when (executionResult) {
            is SuccessExecution -> {
                val stateBefore = ExecutionResultDeserializer.parseMemoryDump(executionResult.stateBefore) ?: return parsingException
                val stateAfter = ExecutionResultDeserializer.parseMemoryDump(executionResult.stateAfter) ?: return parsingException
                PythonEvaluationSuccess(
                    executionResult.isException,
                    calculateCoverage(executionResult.statements, executionResult.missedStatements),
                    stateBefore,
                    stateAfter,
                    executionResult.argsIds + executionResult.kwargsIds,
                    executionResult.resultId,
                )
            }
            is FailExecution -> PythonEvaluationError(
                0,
                executionResult.exception,
                emptyList(),
            )
        }
    }

    private fun calculateCoverage(statements: List<Int>, missedStatements: List<Int>): Coverage {
        val covered = statements.filter { it !in missedStatements }
        return Coverage(
            coveredInstructions=covered.map {
                Instruction(
                    method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                    method.methodSignature(),
                    it,
                    it.toLong()
                )
            },
            instructionsCount = statements.size.toLong(),
            missedInstructions = missedStatements.map {
                Instruction(
                    method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                    method.methodSignature(),
                    it,
                    it.toLong()
                )
            }
        )
    }

    fun stop() {
        executionClient.stopServer()
    }
}