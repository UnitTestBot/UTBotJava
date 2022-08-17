package org.utbot.python.utils

import java.io.BufferedReader
import java.io.InputStreamReader


data class CmdResult(
    val stdout: String,
    val stderr: String,
    val exitValue: Int
)

fun runCommand(command: List<String>): CmdResult {
    val process = ProcessBuilder(command).start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var stdout = ""
    var line: String? = ""
    while (line != null) {
        stdout += line
        line = reader.readLine()
    }
    process.waitFor()
    val stderr = process.errorStream.readBytes().decodeToString().trimIndent()
    return CmdResult(stdout.trimIndent(), stderr, process.exitValue())
}