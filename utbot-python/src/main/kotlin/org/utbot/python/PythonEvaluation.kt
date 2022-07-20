package org.utbot.python

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.UtModel
import java.io.File


sealed class EvaluationResult
data class EvaluationSuccess(val output: OutputData, val isException: Boolean): EvaluationResult()
object EvaluationError : EvaluationResult()

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
        createDirectory(testSourceRoot)

        val outputFilename = "$testSourceRoot/__output_utbot_run_${method.name}.txt"
        val errorFilename = "$testSourceRoot/__error_utbot_run_${method.name}.txt"
        val codeFilename = "$testSourceRoot/__test_utbot_run_${method.name}.py"

        val file = PythonCodeGenerator.generateRunFunctionCode(
            method,
            methodArguments,
            outputFilename,
            errorFilename,
            codeFilename,
            directoriesForSysPath,
            moduleToImport
        )

        val process = Runtime.getRuntime().exec("$pythonPath $codeFilename")
        process.waitFor()
        var failedEvaluation = process.exitValue() != 0

        var outputAsString = ""
        var isSuccess = false

        val resultFile = File(outputFilename)
        if (resultFile.exists()) {
            outputAsString = resultFile.readText()
            resultFile.delete()
            isSuccess = true
        } else {
            val errorFile = File(errorFilename)
            if (errorFile.exists()) {
                outputAsString = errorFile.readText()
                errorFile.delete()
            } else {
                failedEvaluation = true
            }
        }
        file.delete()

        if (failedEvaluation)
            return EvaluationError

        return EvaluationSuccess(
            Klaxon().parse(outputAsString) ?: error("Couldn't parse evaluation output"),
            !isSuccess
        )
    }

    private fun createDirectory(path: String) {
        File(path).mkdir()
    }
}
