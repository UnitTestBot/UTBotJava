package org.utbot.python

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.*
import org.utbot.python.code.KlaxonPythonTreeParser
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.typing.PythonClassIdInfo
import org.utbot.python.utils.FileManager
import org.utbot.python.utils.runCommand


sealed class EvaluationResult
object EvaluationError : EvaluationResult()
class EvaluationSuccess(
    private val output: OutputData,
    private val isException: Boolean,
    private val coverage: Coverage,
): EvaluationResult() {
    operator fun component1() = output
    operator fun component2() = isException
    operator fun component3() = coverage
}

data class OutputData(val output: PythonTree.PythonTreeNode, val type: PythonClassId)

object PythonEvaluation {
    fun evaluate(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        pythonPath: String,
        timeoutForRun: Long,
        additionalModulesToImport: Set<String> = emptySet()
    ): EvaluationResult {
        val runCode = PythonCodeGenerator.generateRunFunctionCode(
            method,
            methodArguments,
            directoriesForSysPath,
            moduleToImport,
            additionalModulesToImport
        )
        val fileWithCode = FileManager.createTemporaryFile(runCode, tag = "run_" + method.name + ".py")
        val result = runCommand(listOf(pythonPath, fileWithCode.path), timeoutForRun)

        if (result.exitValue != 0)
            return EvaluationError

        val output = result.stdout.split('\n')

        if (output.size != 3)
            return EvaluationError

        val status = output[0]

        if (status != PythonCodeGenerator.successStatus && status != PythonCodeGenerator.failStatus)
            return EvaluationError

        val isSuccess = status == PythonCodeGenerator.successStatus

        val pythonTree = KlaxonPythonTreeParser(output[1]).parseJsonToPythonTree()
        val coverage = Coverage(Klaxon().parseArray<Int>(output[2])!!.map {
            Instruction(
                method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                method.methodSignature(),
                it,
                it.toLong()
            )
        })

        return EvaluationSuccess(
            OutputData(pythonTree, pythonTree.type),
            !isSuccess,
            coverage,
        )
    }
}
