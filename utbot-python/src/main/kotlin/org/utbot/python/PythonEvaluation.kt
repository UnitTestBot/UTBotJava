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
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        pythonPath: String,
        additionalModulesToImport: List<String> = emptyList()
    ): EvaluationResult {
        val outputFile = FileManager.assignTemporaryFile(tag = "output_" + method.name)
        val errorFile = FileManager.assignTemporaryFile(tag = "error_" + method.name)
        val runCode = PythonCodeGenerator.generateRunFunctionCode(
            method,
            methodArguments,
            outputFile.path,
            errorFile.path,
            directoriesForSysPath,
            moduleToImport,
            additionalModulesToImport
        )
        val fileWithCode = FileManager.createTemporaryFile(runCode, tag = "run_" + method.name)
        val result = runCommand(listOf(pythonPath, fileWithCode.path))
        var failedEvaluation = result.exitValue != 0

        var outputAsString = ""
        var isSuccess = false

        if (outputFile.exists()) {
            outputAsString = outputFile.readText()
            outputFile.delete()
            isSuccess = true
        } else {
            if (errorFile.exists()) {
                outputAsString = errorFile.readText()
                errorFile.delete()
            } else {
                failedEvaluation = true
            }
        }
        fileWithCode.delete()

        if (failedEvaluation)
            return EvaluationError

        val pythonTree = KlaxonPythonTreeParser.parseJsonToPythonTree(outputAsString)

        return EvaluationSuccess(
            OutputData(pythonTree, pythonTree.type),
            !isSuccess
        )
    }
}
