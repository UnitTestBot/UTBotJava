package org.utbot.cli.go.util

import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream

fun executeCommandAndRedirectStdoutOrFail(
    command: List<String>,
    workingDirectory: File? = null,
    redirectStdoutToStream: OutputStream? = null // if null, stdout of process is suppressed
) {
    val executedProcess = runCatching {
        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true)
            .directory(workingDirectory)
            .start()
        copy(process.inputStream, redirectStdoutToStream)
        process.waitFor()
        process
    }.getOrElse {
        throw RuntimeException(
            "Execution of [${command.joinToString(separator = " ")}] failed with throwable: $it"
        )
    }
    val exitCode = executedProcess.exitValue()
    if (exitCode != 0) {
        val processOutput = InputStreamReader(executedProcess.inputStream).readText()
        throw RuntimeException(
            "Execution of [${command.joinToString(separator = " ")}] failed with non-zero exit code = $exitCode:\n$processOutput"
        )
    }
}