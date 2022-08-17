package org.utbot.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split

class PythonGenerateTestsCommand: CliktCommand(
    name = "generate_python",
    help = "Generate tests for specified Python class or top-level functions from specified file"
) {
    private val sourceFile by argument(
        help = "File with Python code to generate tests for"
    )

    private val pythonClass by option(
        "-c", "--class",
        help = "Specify top-level class under test"
    )

    private val directoriesForSysPath by option(
        "-sp", "--sys-path",
        help = "Directories to add to sys.path"
    ).split(",")

    private val pythonPath by option(
        "-p", "--python-path",
        help = "Path to Python interpreter"
    ).required()

    private val output by option(
        "-o", "--output",
        help = "File for generated tests"
    ).required()

    override fun run() {
        TODO("Not yet implemented")
    }
}