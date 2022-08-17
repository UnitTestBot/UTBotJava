package org.utbot.python

import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.PythonTree
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.code.KlaxonPythonTreeParser
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.typing.PythonClassIdInfo
import org.utbot.python.utils.FileManager
import org.utbot.python.utils.runCommand


sealed class EvaluationResult
object EvaluationError : EvaluationResult()
class EvaluationSuccess(val output: OutputData, val isException: Boolean): EvaluationResult() {
    operator fun component1() = output
    operator fun component2() = isException
}

data class OutputData(val output: PythonTree.PythonTreeNode, val type: PythonClassId)

object PythonEvaluation {
    fun evaluate(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        pythonPath: String,
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
        val result = runCommand(listOf(pythonPath, fileWithCode.path))

        if (result.exitValue != 0)
            return EvaluationError

        val output = result.stdout.split('\n')

        if (output.size != 2)
            return EvaluationError

        val status = output[0]

        if (status != PythonCodeGenerator.successStatus && status != PythonCodeGenerator.failStatus)
            return EvaluationError

        val isSuccess = status == PythonCodeGenerator.successStatus

        val pythonTree = KlaxonPythonTreeParser.parseJsonToPythonTree(output[1])

        return EvaluationSuccess(
            OutputData(pythonTree, pythonTree.type),
            !isSuccess
        )
    }
}
