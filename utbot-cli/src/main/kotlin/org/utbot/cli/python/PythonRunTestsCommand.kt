package org.utbot.cli.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.utbot.framework.codegen.Pytest
import org.utbot.framework.codegen.Unittest
import org.utbot.python.utils.CmdResult
import org.utbot.python.utils.runCommand
import java.io.File
import kotlin.system.exitProcess

class PythonRunTestsCommand : CliktCommand(name = "run_python", help = "Run tests in the specified file") {

    private val sourceFile by argument(
        help = "File with Python tests to run."
    )

    private val pythonPath by option(
        "-p", "--python-path",
        help = "Path to Python interpreter."
    ).required()

    private val output by option(
        "-o", "--output",
        help = "Specify file for report."
    )

    private val testFrameworkAsString by option("--test-framework", help = "Test framework of tests to run")
        .choice(Pytest.toString(), Unittest.toString())
        .default(Unittest.toString())

    private fun runUnittest(): CmdResult =
        runCommand(listOf(
            pythonPath,
            "-m",
            "unittest",
            sourceFile
        ))

    private fun runPytest(): CmdResult =
        runCommand(listOf(
            pythonPath,
            "-m",
            "pytest",
            sourceFile
        ))

    override fun run() {
        val result =
            when (testFrameworkAsString) {
                Unittest.toString() -> runUnittest()
                Pytest.toString() -> runPytest()
                else -> error("Not reachable")
            }

        if (output == null) {
            System.err.println(result.stderr)
            println(result.stdout)
        } else {
            val file = File(output!!)
            file.writeText(result.stderr + result.stdout)
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        exitProcess(result.exitValue)
    }
}