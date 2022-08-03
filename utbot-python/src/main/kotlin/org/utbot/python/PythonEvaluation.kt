package org.utbot.python

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.utils.FileManager
import org.utbot.python.utils.runCommand
import java.io.File


sealed class EvaluationResult
object EvaluationError : EvaluationResult()
class EvaluationSuccess(rawOutput: OutputData, val isException: Boolean): EvaluationResult() {
    val output: OutputData =
        when (rawOutput.type) {
            "complex" -> OutputData("complex('" + rawOutput.output + "')", rawOutput.type)
            else -> rawOutput
        }
    operator fun component1() = output
    operator fun component2() = isException
}

data class OutputData(val output: String, val type: String)

object PythonEvaluation {
    fun evaluate(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        testSourceRoot: String,
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        pythonPath: String
    ): EvaluationResult {
        val outputFile = FileManager.assignTemporaryFile(tag = "output_" + method.name)
        val errorFile = FileManager.assignTemporaryFile(tag = "error_" + method.name)
        val runCode = PythonCodeGenerator.generateRunFunctionCode(
            method,
            methodArguments,
            outputFile.path,
            errorFile.path,
            directoriesForSysPath,
            moduleToImport
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

        return EvaluationSuccess(
            Klaxon().parse(outputAsString) ?: error("Couldn't parse evaluation output"),
            !isSuccess
        )
    }
}
