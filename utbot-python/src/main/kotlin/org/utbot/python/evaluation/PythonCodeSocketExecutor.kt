package org.utbot.python.evaluation

import mu.KotlinLogging
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
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation
import java.net.SocketException

private val logger = KotlinLogging.logger {}

class PythonCodeSocketExecutor(
    override val method: PythonMethod,
    override val moduleToImport: String,
    override val pythonPath: String,
    override val syspathDirectories: Set<String>,
    override val executionTimeout: Long,
) : PythonCodeExecutor {
    private lateinit var pythonWorker: PythonWorker

    constructor(
        method: PythonMethod,
        moduleToImport: String,
        pythonPath: String,
        syspathDirectories: Set<String>,
        executionTimeout: Long,
        pythonWorker: PythonWorker
    ) : this(
        method,
        moduleToImport,
        pythonPath,
        syspathDirectories,
        executionTimeout
    ) {
        this.pythonWorker = pythonWorker
    }

    override fun run(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>
    ): PythonEvaluationResult {
        val (arguments, memory) = serializeObjects(fuzzedValues.allArguments.map { it.tree })

        val containingClass = method.containingPythonClass
        val functionTextName =
            if (containingClass == null)
                method.name
            else {
                val fullname = "${containingClass.pythonTypeName()}.${method.name}"
                fullname.drop(moduleToImport.length).removePrefix(".")
            }

        val request = ExecutionRequest(
            functionTextName,
            moduleToImport,
            additionalModulesToImport.toList(),
            syspathDirectories.toList(),
            arguments,
            memory,
            method.moduleFilename,
        )
        val message = ExecutionRequestSerializer.serializeRequest(request) ?: error("Cannot serialize request to python executor")
        try {
            pythonWorker.sendData(message)
        } catch (_: SocketException) {
            logger.info { "Send data error" }
            return parseExecutionResult(FailExecution("Send data error"))
        }
        val response = pythonWorker.receiveMessage()
        val executionResult = if (response == null) {
            logger.info { "Response error" }
            FailExecution("Execution result error")
        } else {
            ExecutionResultDeserializer.parseExecutionResult(response)
                ?: error("Cannot parse execution result: $response")
        }
        return parseExecutionResult(executionResult)
    }

    private fun parseExecutionResult(executionResult: PythonExecutionResult): PythonEvaluationResult {
        val parsingException = PythonEvaluationError(
            -1,
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
                -2,
                "Fail Execution",
                executionResult.exception.split(System.lineSeparator()),
            )
        }
    }

    private fun calculateCoverage(statements: List<Int>, missedStatements: List<Int>): Coverage {
        val covered = statements.filter { it !in missedStatements }
        return Coverage(
            coveredInstructions=covered.map {
                Instruction(
                    method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
                    method.methodSignature(),
                    it,
                    it.toLong()
                )
            },
            instructionsCount = statements.size.toLong(),
            missedInstructions = missedStatements.map {
                Instruction(
                    method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
                    method.methodSignature(),
                    it,
                    it.toLong()
                )
            }
        )
    }

    override fun stop() {
        pythonWorker.stopServer()
    }
}