package org.utbot.cli.go.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.utbot.cli.go.logic.CliGoUtTestsGenerationController
import org.utbot.cli.go.util.durationInMillis
import org.utbot.cli.go.util.now
import org.utbot.cli.go.util.toAbsolutePath
import org.utbot.go.logic.GoUtTestsGenerationConfig
import org.utbot.go.logic.TestsGenerationMode
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

    private val selectedFunctionNames: List<String> by option(
        "-f", "--function",
        help = StringBuilder()
            .append("Specifies function name to generate tests for. ")
            .append("Can be used multiple times to select multiple functions at the same time.")
            .toString()
    )
        .multiple()

    private val selectedMethodNames: List<String> by option(
        "-m", "--method",
        help = StringBuilder()
            .append("Specifies method name to generate tests for. ")
            .append("Can be used multiple times to select multiple methods at the same time.")
            .toString()
    )
        .multiple()

    private val goExecutablePath: String by option(
        "-go",
        help = "Specifies path to Go executable. For example, it could be [/usr/local/go/bin/go] for some systems"
    )
        .required() // TODO: attempt to find it if not specified

    private val gopath: String by option(
        "-gopath",
        help = buildString {
            appendLine("Specifies path the location of your workspace.")
            appendLine("It defaults to a directory named go inside your home directory, so \$HOME/go on Unix, \$home/go on Plan 9, and %USERPROFILE%\\go (usually C:\\Users\\YourName\\go) on Windows.")
        }
    ).required() // TODO: attempt to find it if not specified

    private val numberOfFuzzingProcesses: Int by option(
        "-parallel",
        help = "The number of fuzzing processes running at once, default 8."
    )
        .int()
        .default(8)
        .check("Must be positive") { it > 0 }

    private val eachFunctionExecutionTimeoutMillis: Long by option(
        "-et", "--each-execution-timeout",
        help = StringBuilder()
            .append("Specifies a timeout in milliseconds for each fuzzed function execution.")
            .append("Default is ${GoUtTestsGenerationConfig.DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS} ms")
            .toString()
    )
        .long()
        .default(GoUtTestsGenerationConfig.DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS)
        .check("Must be positive") { it > 0 }

    private val allFunctionExecutionTimeoutMillis: Long by option(
        "-at", "--all-execution-timeout",
        help = StringBuilder()
            .append("Specifies a timeout in milliseconds for all fuzzed function execution.")
            .append("Default is ${GoUtTestsGenerationConfig.DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS} ms")
            .toString()
    )
        .long()
        .default(GoUtTestsGenerationConfig.DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS)
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

    private val fuzzingMode: Boolean by option(
        "-fm",
        "--fuzzing-mode",
        help = "Stop test generation when a panic or error occurs (only one test will be generated for one of these cases)"
    )
        .flag(default = false)

    override fun run() {
        if (selectedFunctionNames.isEmpty() && selectedMethodNames.isEmpty()) {
            throw IllegalArgumentException("Functions or methods must be passed")
        }

        val sourceFileAbsolutePath = sourceFile.toAbsolutePath()
        val goExecutableAbsolutePath = goExecutablePath.toAbsolutePath()
        val gopathAbsolutePath = gopath.toAbsolutePath()
        val mode = if (fuzzingMode) {
            TestsGenerationMode.FUZZING_MODE
        } else {
            TestsGenerationMode.DEFAULT
        }

        val testsGenerationStarted = now()
        logger.info { "Test file generation for [$sourceFile] - started" }
        try {
            CliGoUtTestsGenerationController(
                printToStdOut = printToStdOut,
                overwriteTestFiles = overwriteTestFiles
            ).generateTests(
                mapOf(sourceFileAbsolutePath to selectedFunctionNames),
                mapOf(sourceFileAbsolutePath to selectedMethodNames),
                GoUtTestsGenerationConfig(
                    goExecutableAbsolutePath,
                    gopathAbsolutePath,
                    numberOfFuzzingProcesses,
                    mode,
                    eachFunctionExecutionTimeoutMillis,
                    allFunctionExecutionTimeoutMillis
                ),
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