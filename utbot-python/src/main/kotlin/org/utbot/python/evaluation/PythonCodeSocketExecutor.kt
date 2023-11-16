package org.utbot.python.evaluation

import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.evaluation.serialization.ExecutionRequest
import org.utbot.python.evaluation.serialization.ExecutionRequestSerializer
import org.utbot.python.evaluation.serialization.ExecutionResultDeserializer
import org.utbot.python.evaluation.serialization.FailExecution
import org.utbot.python.evaluation.serialization.PythonExecutionResult
import org.utbot.python.evaluation.serialization.SuccessExecution
import org.utbot.python.evaluation.serialization.serializeObjects
import org.utbot.python.coverage.CoverageIdGenerator
import org.utbot.python.coverage.toPyInstruction
import org.utbot.python.evaluation.serialization.MemoryMode
import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.pythonDescription
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.utils.isNamed
import java.net.SocketException

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
        val coverageId = CoverageIdGenerator.createId()
        return runWithCoverage(fuzzedValues, additionalModulesToImport, coverageId)
    }

    override fun runWithCoverage(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>,
        coverageId: String
    ): PythonEvaluationResult {
        val (arguments, memory) = serializeObjects(fuzzedValues.allArguments.map { it.tree })

        val meta = method.definition.type.pythonDescription() as PythonCallableTypeDescription
        val argKinds = meta.argumentKinds
        val namedArgs = meta.argumentNames
            .filterIndexed { index, _ -> !isNamed(argKinds[index]) }

        val (positionalArguments, namedArguments) = arguments
            .zip(meta.argumentNames)
            .partition { (_, name) ->
                 namedArgs.contains(name)
            }

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
            positionalArguments.map { it.first },
            namedArguments.associate { it.second!! to it.first },  // here can be only-kwargs arguments
            memory,
            MemoryMode.REDUCE,
            method.moduleFilename,
            coverageId,
        )
        val message = ExecutionRequestSerializer.serializeRequest(request) ?: error("Cannot serialize request to python executor")
        try {
            pythonWorker.sendData(message)
        } catch (_: SocketException) {
            return parseExecutionResult(FailExecution("Send data error"))
        }

        val (status, response) = UtExecutorThread.run(pythonWorker, executionTimeout)
        return when (status) {
            UtExecutorThread.Status.TIMEOUT -> {

                PythonEvaluationTimeout()
            }

            UtExecutorThread.Status.OK -> {
                val executionResult = response?.let {
                    ExecutionResultDeserializer.parseExecutionResult(it)
                        ?: error("Cannot parse execution result: $it")
                } ?: FailExecution("Execution result error")

                parseExecutionResult(executionResult)
            }
        }
    }

    override fun runWithCoverage(pickledArguments: String, coverageId: String): PythonEvaluationResult {
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
            emptyList(),
            syspathDirectories.toList(),
            emptyList(),
            emptyMap(),
            pickledArguments,
            MemoryMode.PICKLE,
            method.moduleFilename,
            coverageId,
        )
        val message = ExecutionRequestSerializer.serializeRequest(request) ?: error("Cannot serialize request to python executor")
        try {
            pythonWorker.sendData(message)
        } catch (_: SocketException) {
            return parseExecutionResult(FailExecution("Send data error"))
        }

        val (status, response) = UtExecutorThread.run(pythonWorker, executionTimeout)
        return when (status) {
            UtExecutorThread.Status.TIMEOUT -> {
                PythonEvaluationTimeout()
            }

            UtExecutorThread.Status.OK -> {
                val executionResult = response?.let {
                    ExecutionResultDeserializer.parseExecutionResult(it)
                        ?: error("Cannot parse execution result: $it")
                } ?: FailExecution("Execution result error")

                parseExecutionResult(executionResult)
            }
        }
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
                val stateInit = ExecutionResultDeserializer.parseMemoryDump(executionResult.stateInit) ?: stateBefore
                val diffIds = executionResult.diffIds.map {it.toLong()}
                val statements = executionResult.statements.mapNotNull { it.toPyInstruction() }
                val missedStatements = executionResult.missedStatements.mapNotNull { it.toPyInstruction() }
                PythonEvaluationSuccess(
                    executionResult.isException,
                    statements,
                    missedStatements,
                    stateInit,
                    stateBefore,
                    stateAfter,
                    diffIds,
                    executionResult.argsIds + executionResult.kwargsIds.values,
                    executionResult.resultId,
                )
            }
            is FailExecution -> {
                PythonEvaluationError(
                    -2,
                    "Fail Execution",
                    executionResult.exception.split(System.lineSeparator()),
                )
            }
        }
    }

    override fun stop() {
        pythonWorker.stopServer()
    }
}