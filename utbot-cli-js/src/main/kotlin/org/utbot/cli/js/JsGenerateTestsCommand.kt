package org.utbot.cli.js

import api.JsTestGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import mu.KotlinLogging
import org.utbot.cli.js.JsUtils.makeAbsolutePath
import service.coverage.CoverageMode
import settings.JsDynamicSettings
import settings.JsExportsSettings.endComment
import settings.JsExportsSettings.startComment
import settings.JsTestGenerationSettings.defaultTimeout
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}


class JsGenerateTestsCommand :
    CliktCommand(name = "generate_js", help = "Generates tests for the specified class or toplevel functions.") {

    private val sourceCodeFile by option(
        "-s", "--source",
        help = "Specifies source code file for a generated test."
    )
        .required()
        .check("Must exist and ends with .js suffix") {
            it.endsWith(".js") && Files.exists(Paths.get(it))
        }

    private val targetClass by option("-c", "--class", help = "Specifies target class to generate tests for.")

    private val output by option("-o", "--output", help = "Specifies output file for generated tests.")
        .check("Must end with .js suffix") {
            it.endsWith(".js")
        }

    private val printToStdOut by option(
        "-p",
        "--print-test",
        help = "Specifies whether test should be printed out to StdOut."
    )
        .flag(default = false)

    private val timeout by option(
        "-t",
        "--timeout",
        help = "Timeout for Node.js to run scripts in seconds."
    ).default("$defaultTimeout")

    private val coverageMode by option(
        "--coverage-mode",
        help = "Specifies the coverage mode for test generation. Check docs for more info."
    ).choice(
        CoverageMode.BASIC.toString() to CoverageMode.BASIC,
        CoverageMode.FAST.toString() to CoverageMode.FAST
    ).default(CoverageMode.FAST)

    private val pathToNode by option(
        "--path-to-node",
        help = "Sets path to Node.js executable, defaults to \"node\" shortcut."
    ).default("node")

    private val pathToNYC by option(
        "--path-to-nyc",
        help = "Sets path to nyc executable, defaults to \"nyc\" shortcut. " +
                "As there are many nyc files in the global npm directory, choose one without file extension."
    ).default("nyc")

    private val pathToNPM by option(
        "--path-to-npm",
        help = "Sets path to npm executable, defaults to \"npm\" shortcut."
    ).default("npm")

    override fun run() {
        val started = LocalDateTime.now()
        try {
            val sourceFileAbsolutePath = makeAbsolutePath(sourceCodeFile)
            logger.info { "Generating tests for [$sourceFileAbsolutePath] - started" }
            val fileText = File(sourceCodeFile).readText()
            currentFileText = fileText
            val outputAbsolutePath = output?.let { makeAbsolutePath(it) }
            val testGenerator = JsTestGenerator(
                fileText = fileText,
                sourceFilePath = sourceFileAbsolutePath,
                parentClassName = targetClass,
                outputFilePath = outputAbsolutePath,
                exportsManager = ::manageExports,
                settings = JsDynamicSettings(
                    pathToNode = pathToNode,
                    pathToNYC = pathToNYC,
                    pathToNPM = pathToNPM,
                    timeout = timeout.toLong(),
                    coverageMode = coverageMode,

                    )
            )
            val testCode = testGenerator.run()

            if (printToStdOut || (outputAbsolutePath == null && !printToStdOut)) {
                logger.info { "\n$testCode" }
            }
            outputAbsolutePath?.let { filePath ->
                val outputFile = File(filePath)
                outputFile.createNewFile()
                outputFile.writeText(testCode)
            }

        } catch (t: Throwable) {
            logger.error { "An error has occurred while generating tests for file $sourceCodeFile : $t" }
            throw t
        } finally {
            val duration = ChronoUnit.MILLIS.between(started, LocalDateTime.now())
            logger.debug { "Generating test for [$sourceCodeFile] - completed in [$duration] (ms)" }
        }
    }

    // Needed for continuous exports managing
    private var currentFileText = ""

    private fun manageExports(swappedText: (String?, String) -> String) {
        val file = File(sourceCodeFile)
        when {

            currentFileText.contains(startComment) -> {
                val regex = Regex("$startComment((\\r\\n|\\n|\\r|.)*)$endComment")
                regex.find(currentFileText)?.groups?.get(1)?.value?.let { existingSection ->
                    val newText = swappedText(existingSection, currentFileText)
                    file.writeText(newText)
                    currentFileText = newText
                }
            }

            else -> {
                val line = buildString {
                    append("\n$startComment")
                    append(swappedText(null, currentFileText))
                    append(endComment)
                }
                file.appendText(line)
                currentFileText = file.readText()
            }
        }
    }
}
