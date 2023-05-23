package org.utbot.go.util

import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path

fun modifyEnvironment(goExecutableAbsolutePath: Path, gopathAbsolutePath: Path): MutableMap<String, String> {
    val environment = System.getenv().toMutableMap().apply {
        this["Path"] = goExecutableAbsolutePath.resolve(File.pathSeparator).toString() + (this["Path"] ?: "")
        this["GOROOT"] = goExecutableAbsolutePath.parent.parent.toString()
        this["GOPATH"] = gopathAbsolutePath.toString()
    }
    return environment
}

fun executeCommandByNewProcessOrFail(
    command: List<String>,
    workingDirectory: File,
    executionTargetName: String,
    environment: Map<String, String> = System.getenv(),
    helpMessage: String? = null
) {
    val helpMessageLine = if (helpMessage == null) "" else "\n\nHELP: $helpMessage"
    val executedProcess = runCatching {
        val process = executeCommandByNewProcessOrFailWithoutWaiting(command, workingDirectory, environment)
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

fun executeCommandByNewProcessOrFailWithoutWaiting(
    command: List<String>,
    workingDirectory: File,
    environment: Map<String, String> = System.getenv()
): Process {
    val processBuilder = ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectErrorStream(true)
        .directory(workingDirectory)
    processBuilder.environment().clear()
    processBuilder.environment().putAll(environment)
    return processBuilder.start()
}