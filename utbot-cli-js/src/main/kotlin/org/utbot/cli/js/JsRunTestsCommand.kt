package org.utbot.cli.js

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KotlinLogging
import org.utbot.cli.js.JsUtils.makeAbsolutePath
import utils.JsCmdExec
import java.io.File

private val logger = KotlinLogging.logger {}

class JsRunTestsCommand : CliktCommand(name = "run_js", help = "Runs tests for the specified file or directory.") {

    private val fileWithTests by option(
        "--fileOrDir", "-f",
        help = "Specifies a file or directory with tests."
    ).required()

    private val output by option(
        "-o", "--output",
        help = "Specifies an output .txt file for test framework result."
    ).check("Must end with .txt suffix") {
        it.endsWith(".txt")
    }

    private val testFramework by option("--test-framework", "-t", help = "Test framework to be used.")
        .choice("mocha")
        .default("mocha")


    override fun run() {
        val fileWithTestsAbsolutePath = makeAbsolutePath(fileWithTests)
        val dir = if (fileWithTestsAbsolutePath.endsWith(".js"))
            fileWithTestsAbsolutePath.substringBeforeLast("/") else fileWithTestsAbsolutePath
        val outputAbsolutePath = output?.let { makeAbsolutePath(it) }
        when (testFramework) {
            "mocha" -> {
                val (inputText, errorText) = JsCmdExec.runCommand(
                    dir = dir,
                    shouldWait = true,
                    cmd = arrayOf("mocha", fileWithTestsAbsolutePath)
                )
                if (errorText.isNotEmpty()) {
                    logger.error { "An error has occurred while running tests for $fileWithTests : $errorText" }
                } else {
                    outputAbsolutePath?.let {
                        val file = File(it)
                        file.createNewFile()
                        file.writeText(inputText)
                    } ?: logger.info { "\n$inputText" }
                }
            }
        }
    }
}
