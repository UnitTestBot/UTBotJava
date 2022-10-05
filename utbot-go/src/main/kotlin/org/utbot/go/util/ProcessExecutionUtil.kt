package org.utbot.go.util

import java.io.File
import java.io.InputStreamReader

fun executeCommandByNewProcessOrFail(command: List<String>, workingDirectory: File, executionTargetName: String) {
    val executedProcess = runCatching {
        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true)
            .directory(workingDirectory)
            .start()
        process.waitFor()
        process
    }.getOrElse {
        throw RuntimeException(
            "Execution of $executionTargetName in child process failed with throwable: $it"
        )
    }
    val exitCode = executedProcess.exitValue()
    if (exitCode != 0) {
        val processOutput = InputStreamReader(executedProcess.inputStream).readText()
        throw RuntimeException(
            "Execution of $executionTargetName in child process failed with non-zero exit code = $exitCode:\n$processOutput"
        )
    }
}