package org.utbot.cli.go.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.utbot.cli.go.logic.CliGoUtTestsGenerationController
import org.utbot.cli.go.util.durationInMillis
import org.utbot.cli.go.util.now
import org.utbot.cli.go.util.toAbsolutePath
import org.utbot.go.logic.GoUtTestsGenerationConfig
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class GenerateGoTestsCommand :
    CliktCommand(name = "generateGo", help = "Generates tests for the specified Go source file") {

    private val sourceFile: String by option(
        "-s", "--source",
        help = "Specifies Go source file to generate tests for"
    )
        .required()
        .check("Must exist and ends with *.go suffix") {
            it.endsWith(".go") && Files.exists(Paths.get(it))
        }

    private val selectedFunctionsNames: List<String> by option(
        "-f", "--function",
        help = StringBuilder()
            .append("Specifies function name to generate tests for. ")
            .append("Can be used multiple times to select multiple functions at the same time.")
            .toString()
    )
        .multiple(required = true)

    private val goExecutablePath: String by option(
        "-go", "--go-path",
        help = "Specifies path to Go executable. For example, it could be [/usr/local/go/bin/go] for some systems"
    )
        .required() // TODO: attempt to find it if not specified

    private val eachFunctionExecutionTimeoutMillis: Long by option(
        "-t", "--each-execution-timeout",
        help = StringBuilder()
            .append("Specifies a timeout in milliseconds for each fuzzed function execution.")
            .append("Default is ${GoUtTestsGenerationConfig.DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS} ms")
            .toString()
    )
        .long()
        .default(GoUtTestsGenerationConfig.DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS)
        .check("Must be positive") { it > 0 }

    private val printToStdOut: Boolean by option(
        "-p",
        "--print-test",
        help = "Specifies whether a test should be printed out to StdOut. Is disabled by default"
    )
        .flag(default = false)

    private val overwriteTestFiles: Boolean by option(
        "-w",
        "--overwrite",
        help = "Specifies whether to overwrite the output test file if it already exists. Is disabled by default"
    )
        .flag(default = false)

    override fun run() {
        val sourceFileAbsolutePath = sourceFile.toAbsolutePath()
        val goExecutableAbsolutePath = goExecutablePath.toAbsolutePath()

        val testsGenerationStarted = now()
        logger.info { "Test file generation for [$sourceFile] - started" }
        try {
            CliGoUtTestsGenerationController(
                printToStdOut = printToStdOut,
                overwriteTestFiles = overwriteTestFiles
            ).generateTests(
                mapOf(sourceFileAbsolutePath to selectedFunctionsNames),
                GoUtTestsGenerationConfig(goExecutableAbsolutePath, eachFunctionExecutionTimeoutMillis)
            )
        } catch (t: Throwable) {
            logger.error { "An error has occurred while generating test for snippet $sourceFile: $t" }
            throw t
        } finally {
            val duration = durationInMillis(testsGenerationStarted)
            logger.info { "Test file generation for [$sourceFile] - completed in [$duration] (ms)" }
        }
    }
}