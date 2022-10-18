package org.utbot.cli.js

import api.JsTestGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import mu.KotlinLogging
import org.utbot.cli.js.JsUtils.makeAbsolutePath
import service.CoverageMode
import settings.JsDynamicSettings
import settings.JsExportsSettings.endComment
import settings.JsExportsSettings.startComment
import settings.JsTestGenerationSettings.defaultTimeout

private val logger = KotlinLogging.logger {}


class JsGenerateTestsCommand :
    CliktCommand(name = "generate_js", help = "Generates tests for the specified class or toplevel functions") {

    private val sourceCodeFile by option(
        "-s", "--source",
        help = "Specifies source code file for a generated test"
    )
        .required()
        .check("Must exist and ends with .js suffix") {
            it.endsWith(".js") && Files.exists(Paths.get(it))
        }

    private val targetClass by option("-c", "--class", help = "Specifies target class to generate tests for")

    private val output by option("-o", "--output", help = "Specifies output file for generated tests")
        .check("Must end with .js suffix") {
            it.endsWith(".js")
        }

    private val printToStdOut by option(
        "-p",
        "--print-test",
        help = "Specifies whether test should be printed out to StdOut"
    )
        .flag(default = false)

    private val timeout by option(
        "-t",
        "--timeout",
        help = "Timeout for Node.js to run scripts in seconds"
    ).default("$defaultTimeout")

    private val coverageMode by option(
        "--coverage-mode",
        help = "Specifies the coverage mode for test generation. Check docs for more info"
    ).choice(
        CoverageMode.BASIC.toString() to CoverageMode.BASIC,
        CoverageMode.FAST.toString() to CoverageMode.FAST
    ).default(CoverageMode.FAST)

    private val pathToNode by option(
        "--path-to-node",
        help = "Sets path to Node.js executable, defaults to \"node\" shortcut"
    ).default("node")

    private val pathToNYC by option(
        "--path-to-nyc",
        help = "Sets path to nyc executable, defaults to \"nyc\" shortcut. " +
                "As there are many nyc files in the global npm directory, choose one without file extension"
    ).default("nyc")

    private val pathToNPM by option(
        "--path-to-npm",
        help = "Sets path to npm executable, defaults to \"npm\" shortcut."
    ).default("npm")

    override fun run() {
        val started = LocalDateTime.now()
        try {
            logger.info { "Generating test for [$sourceCodeFile] - started" }
            val fileText = File(sourceCodeFile).readText()
            val outputAbsolutePath = output?.let { makeAbsolutePath(it) }
            val testGenerator = JsTestGenerator(
                fileText = fileText,
                sourceFilePath = makeAbsolutePath(sourceCodeFile),
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
                logger.info { filePath }
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

    private fun manageExports(exports: List<String>) {
        val exportSection = exports.joinToString("\n") { "exports.$it = $it" }
        val file = File(sourceCodeFile)
        val fileText = file.readText()
        when {
            fileText.contains(exportSection) -> {}

            fileText.contains(startComment) && !fileText.contains(exportSection) -> {
                val regex = Regex("\n$startComment\n(.|\n)*\n$endComment")
                regex.find(fileText)?.groups?.get(1)?.value?.let {existingSection ->
                    val existingExports = existingSection.split("\n")
                    val exportRegex = Regex("exports.(.*) =")
                    val existingExportsSet = existingExports.map { rawLine ->
                        exportRegex.find(rawLine)?.groups?.get(1)?.value ?: throw IllegalStateException()
                    }.toSet()
                    val resultSet = existingExportsSet + exports.toSet()
                    val resSection = resultSet.joinToString("\n") { "exports.$it = $it" }
                    val swappedText = fileText.replace(existingSection, resSection)
                    file.writeText(swappedText)
                }
            }

            else -> {
                val line = buildString {
                    append("\n$startComment\n")
                    append(exportSection)
                    append("\n$endComment")
                }
                file.appendText(line)
            }
        }
    }
}