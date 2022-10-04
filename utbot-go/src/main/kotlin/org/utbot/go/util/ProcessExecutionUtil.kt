package org.utbot.go.util

import java.io.File
import java.io.InputStreamReader

fun executeCommandByNewProcessOrFail(
    command: List<String>,
    workingDirectory: File,
    executionTargetName: String,
    helpMessage: String? = null
) {
    val helpMessageLine = if (helpMessage == null) "" else "\n\nHELP: $helpMessage"
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
            StringBuilder()
                .append("Execution of $executionTargetName in child process failed with throwable: ")
                .append("$it").append(helpMessageLine)
                .toString()
        )
    }
    val exitCode = executedProcess.exitValue()
    if (exitCode != 0) {
        val processOutput = InputStreamReader(executedProcess.inputStream).readText()
        throw RuntimeException(
            StringBuilder()
                .append("Execution of $executionTargetName in child process failed with non-zero exit code = $exitCode: ")
                .append("\n$processOutput").append(helpMessageLine)
                .toString()
        )
    }
}