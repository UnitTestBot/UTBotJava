package org.utbot.python.evaluation

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.evaluation.serialiation.ExecutionResultDeserializer
import org.utbot.python.evaluation.serialiation.FailExecution
import org.utbot.python.evaluation.serialiation.SuccessExecution
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.io.File

data class EvaluationFiles(
    val executionFile: File,
    val fileForOutput: File,
)

class PythonCodeExecutorImpl(
    override val method: PythonMethod,
    override val moduleToImport: String,
    override val pythonPath: String,
    override val syspathDirectories: Set<String>,
    override val executionTimeout: Long,
) : PythonCodeExecutor {

    override fun run(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>,
    ): PythonEvaluationResult {
        val evaluationFiles = generateExecutionCode(
            additionalModulesToImport,
            fuzzedValues.allArguments,
        )
        return getEvaluationResult(evaluationFiles)
    }

    private fun generateExecutionCode(
        additionalModulesToImport: Set<String>,
        methodArguments: List<PythonTreeModel>,
    ): EvaluationFiles {
        val fileForOutput = TemporaryFileManager.assignTemporaryFile(
            tag = "out_" + method.name + ".py",
            addToCleaner = false
        )
        val coverageDatabasePath = TemporaryFileManager.assignTemporaryFile(
            tag = "coverage_db_" + method.name,
            addToCleaner = false,
        )
        val runCode = PythonCodeGenerator.generateRunFunctionCode(
            method,
            methodArguments,
            syspathDirectories,
            moduleToImport,
            additionalModulesToImport,
            fileForOutput.path,
            coverageDatabasePath.path,
        )
        val executionFile = TemporaryFileManager.createTemporaryFile(
            runCode,
            tag = "run_" + method.name + ".py",
            addToCleaner = false
        )
        return EvaluationFiles(executionFile, fileForOutput)
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

    private fun getEvaluationResult(evaluationFiles: EvaluationFiles): PythonEvaluationResult {
        val evaluationProcess = startProcess(listOf(pythonPath, evaluationFiles.executionFile.path))
        val result = getResult(evaluationProcess, timeout = executionTimeout)

        if (result.terminatedByTimeout)
            return PythonEvaluationTimeout()

        if (result.exitValue != 0)
            return PythonEvaluationError(
                result.exitValue,
                result.stdout,
                result.stderr.split(System.lineSeparator())
            )

        val content = evaluationFiles.fileForOutput.readText()
        evaluationFiles.fileForOutput.delete()

        return parseExecutionResult(content)
    }

    private fun parseExecutionResult(content: String): PythonEvaluationResult {
        val parsingException = PythonEvaluationError(
            0,
            "Incorrect format of output",
            emptyList()
        )
        val executionResult = ExecutionResultDeserializer.parseExecutionResult(content) ?: return parsingException
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
}