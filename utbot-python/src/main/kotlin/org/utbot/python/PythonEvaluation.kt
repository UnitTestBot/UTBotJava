package org.utbot.python

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.coroutineScope
import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedValue
import org.utbot.python.code.KlaxonPythonTreeParser
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.utils.FileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.runCommand
import org.utbot.python.utils.startProcess


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

fun startEvaluationProcess(input: EvaluationInput): Process {
    val runCode = PythonCodeGenerator.generateRunFunctionCode(
        input.method,
        input.methodArguments,
        input.directoriesForSysPath,
        input.moduleToImport,
        input.additionalModulesToImport
    )
    val fileWithCode = FileManager.createTemporaryFile(runCode, tag = "run_" + input.method.name + ".py")
    return startProcess(listOf(input.pythonPath, fileWithCode.path))
}

fun getEvaluationResult(input: EvaluationInput, process: Process, timeout: Long): EvaluationResult {
    val result = getResult(process, timeout = timeout)

    if (result.exitValue != 0)
        return EvaluationError(
            if (result.terminatedByTimeout) "Timeout" else "Non-zero exit status"
        )

    val output = result.stdout.split('\n')

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
