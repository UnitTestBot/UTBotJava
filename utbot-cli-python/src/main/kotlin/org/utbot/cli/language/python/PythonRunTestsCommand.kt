package org.utbot.cli.language.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.utils.CmdResult
import org.utbot.python.utils.runCommand
import java.io.File
import java.nio.file.Paths

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

    private fun runUnittest(): CmdResult {
        val currentPath = Paths.get("").toAbsolutePath().toString()
        val sourceFilePath = Paths.get(sourceFile).toAbsolutePath().toString()
        return if (sourceFilePath.startsWith(currentPath)) {
            runCommand(
                listOf(
                    pythonPath,
                    "-m",
                    "unittest",
                    sourceFile
                )
            )
        } else CmdResult(
            "",
            "File $sourceFile can not be imported from Unittest. Move test file to child directory or use pytest.",
            1
        )
    }

    private fun runPytest(): CmdResult =
        runCommand(
            listOf(
                pythonPath,
                "-m",
                "pytest",
                sourceFile
            )
        )

    override fun run() {
        val result =
            when (testFrameworkAsString) {
                Unittest.toString() -> runUnittest()
                Pytest.toString() -> runPytest()
                else -> error("Not reachable")
            }

        output?.let {
            val file = File(it)
            file.writeText(result.stderr + result.stdout)
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        println(result.stderr)
        println(result.stdout)
    }
}