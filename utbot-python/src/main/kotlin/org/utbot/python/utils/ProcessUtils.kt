package org.utbot.python.utils

data class CmdResult(
    val stdout: String,
    val stderr: String,
    val exitValue: Int
)

fun runCommand(command: List<String>): CmdResult {
    val process = ProcessBuilder(command).start()
    process.waitFor()
    val stdout = process.inputStream.readBytes().decodeToString().trimIndent()
    val stderr = process.errorStream.readBytes().decodeToString().trimIndent()
    return CmdResult(stdout, stderr, process.exitValue())
}