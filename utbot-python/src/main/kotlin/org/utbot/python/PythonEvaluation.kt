package org.utbot.python

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.python.PythonClassId
import org.utbot.framework.plugin.api.python.PythonTree
import org.utbot.framework.plugin.api.python.pythonAnyClassId
import org.utbot.fuzzer.FuzzedValue
import org.utbot.python.code.KlaxonPythonTreeParser
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.io.File


sealed class EvaluationResult
class EvaluationError(val reason: String) : EvaluationResult()
class EvaluationSuccess(
    private val output: OutputData,
    private val isException: Boolean,
    val coverage: PythonCoverage
): EvaluationResult() {
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

data class EvaluationProcess (
    val process: Process,
    val fileWithCode: File,
    val fileForOutput: File
)

fun startEvaluationProcess(input: EvaluationInput): EvaluationProcess {
    val fileForOutput = TemporaryFileManager.assignTemporaryFile(
        tag = "out_" + input.method.name + ".py",
        addToCleaner = false
    )
    val runCode = PythonCodeGenerator.generateRunFunctionCode(
        input.method,
        input.methodArguments,
        input.directoriesForSysPath,
        input.moduleToImport,
        input.additionalModulesToImport,
        fileForOutput.path
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

fun getEvaluationResult(input: EvaluationInput, process: EvaluationProcess, timeout: Long): EvaluationResult {
    val result = getResult(process.process, timeout = timeout)
    process.fileWithCode.delete()

    if (result.exitValue != 0)
        return EvaluationError(
            if (result.terminatedByTimeout) "Timeout" else "Non-zero exit status"
        )

    val output = process.fileForOutput.readText().split('\n')
    process.fileForOutput.delete()

    if (output.size != 4)
        return EvaluationError("Incorrect format of output")

    val status = output[0]

    if (status != PythonCodeGenerator.successStatus && status != PythonCodeGenerator.failStatus)
        return EvaluationError("Incorrect format of output")

    val isSuccess = status == PythonCodeGenerator.successStatus

    val pythonTree = KlaxonPythonTreeParser(output[1]).parseJsonToPythonTree()
    val stmts = Klaxon().parseArray<Int>(output[2])!!
    val missed = Klaxon().parseArray<Int>(output[3])!!
    val covered = stmts.filter { it !in missed }
    val coverage = PythonCoverage(
        covered.map {
            Instruction(
                input.method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                input.method.methodSignature(),
                it,
                it.toLong()
            )
        },
        missed.map {
            Instruction(
                input.method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                input.method.methodSignature(),
                it,
                it.toLong()
            )
        }
    )

    return EvaluationSuccess(
        OutputData(pythonTree, pythonTree.type),
        !isSuccess,
        coverage
    )
}
