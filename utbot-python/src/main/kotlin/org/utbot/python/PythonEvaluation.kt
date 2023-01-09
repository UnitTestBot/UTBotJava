package org.utbot.python

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.python.code.KlaxonPythonTreeParser
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.io.File

sealed class PythonEvaluationResult

class PythonEvaluationError(
    val status: Int,
    val message: String,
    val stackTrace: List<String>
) : PythonEvaluationResult()

class PythonEvaluationTimeout(
    val message: String = "Timeout"
) : PythonEvaluationResult()

class PythonEvaluationSuccess(
    private val output: OutputData,
    private val isException: Boolean,
    val coverage: Coverage
) : PythonEvaluationResult() {
    operator fun component1() = output
    operator fun component2() = isException
    operator fun component3() = coverage
}

data class OutputData(val output: PythonTree.PythonTreeNode, val type: PythonClassId)

data class EvaluationInput(
    val method: PythonMethod,
    val methodArguments: List<UtModel>,
    val directoriesForSysPath: Set<String>,
    val moduleToImport: String,
    val pythonPath: String,
    val timeoutForRun: Long,
    val thisObject: UtModel?,
    val modelList: List<UtModel>,
    val values: List<FuzzedValue>,
    val additionalModulesToImport: Set<String> = emptySet()
)

data class EvaluationProcess(
    val process: Process,
    val fileWithCode: File,
    val fileForOutput: File
)

fun startEvaluationProcess(input: EvaluationInput): EvaluationProcess {
    val fileForOutput = TemporaryFileManager.assignTemporaryFile(
        tag = "out_" + input.method.name + ".py",
        addToCleaner = false
    )
    val coverageDatabasePath = TemporaryFileManager.assignTemporaryFile(
        tag = "coverage_db_" + input.method.name,
        addToCleaner = false,
    )
    val runCode = PythonCodeGenerator.generateRunFunctionCode(
        input.method,
        input.methodArguments,
        input.directoriesForSysPath,
        input.moduleToImport,
        input.additionalModulesToImport,
        fileForOutput.path.replace("\\", "\\\\"),
        coverageDatabasePath.absolutePath.replace("\\", "\\\\")
    )
    val fileWithCode = TemporaryFileManager.createTemporaryFile(
        runCode,
        tag = "run_" + input.method.name + ".py",
        addToCleaner = false
    )
    return EvaluationProcess(
        startProcess(listOf(input.pythonPath, fileWithCode.path)),
        fileWithCode,
        fileForOutput
    )
}

fun calculateCoverage(statements: List<Int>, missedStatements: List<Int>, input: EvaluationInput): Coverage {
    val covered = statements.filter { it !in missedStatements }
    return Coverage(
        coveredInstructions=covered.map {
            Instruction(
                input.method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                input.method.methodSignature(),
                it,
                it.toLong()
            )
        },
        instructionsCount = statements.size.toLong(),
        missedInstructions = missedStatements.map {
            Instruction(
                input.method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                input.method.methodSignature(),
                it,
                it.toLong()
            )
        }
    )
}

fun getEvaluationResult(input: EvaluationInput, process: EvaluationProcess, timeout: Long): PythonEvaluationResult {
    val result = getResult(process.process, timeout = timeout)
    process.fileWithCode.delete()

    if (result.terminatedByTimeout)
        return PythonEvaluationTimeout()

    if (result.exitValue != 0)
        return PythonEvaluationError(
            result.exitValue,
            result.stdout,
            result.stderr.split("\n")
        )

    val output = process.fileForOutput.readText().split(System.lineSeparator())
    process.fileForOutput.delete()

    if (output.size != 4)
        return PythonEvaluationError(
            0,
            "Incorrect format of output",
            emptyList()
        )

    val status = output[0]

    if (status != PythonCodeGenerator.successStatus && status != PythonCodeGenerator.failStatus)
        return PythonEvaluationError(
            0,
            "Incorrect format of output",
            emptyList()
        )

    val isSuccess = status == PythonCodeGenerator.successStatus

    val pythonTree = KlaxonPythonTreeParser(output[1]).parseJsonToPythonTree()
    val stmts = Klaxon().parseArray<Int>(output[2])!!
    val missed = Klaxon().parseArray<Int>(output[3])!!

    val coverage = calculateCoverage(stmts, missed, input)

    return PythonEvaluationSuccess(
        OutputData(pythonTree, pythonTree.type),
        !isSuccess,
        coverage
    )
}
