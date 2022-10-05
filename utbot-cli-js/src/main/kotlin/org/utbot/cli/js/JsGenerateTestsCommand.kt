package org.utbot.cli.js

import api.JsTestGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import mu.KotlinLogging
import org.utbot.cli.js.JsUtils.makeAbsolutePath
import settings.JsExportsSettings.endComment
import settings.JsExportsSettings.exportsLinePrefix
import settings.JsExportsSettings.startComment
import settings.JsTestGenerationSettings.defaultTimeout
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

    override fun run() {
        val started = LocalDateTime.now()
        try {
            logger.debug { "Installing npm packages" }
            logger.debug { "Generating test for [$sourceCodeFile] - started" }
            val fileText = File(sourceCodeFile).readText()
            val outputAbsolutePath = output?.let { makeAbsolutePath(it) }
            val testGenerator = JsTestGenerator(
                fileText = fileText,
                sourceFilePath = makeAbsolutePath(sourceCodeFile),
                parentClassName = targetClass,
                outputFilePath = outputAbsolutePath,
                exportsManager = ::manageExports,
                timeout = timeout.toLong()
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

    private fun manageExports(exports: List<String>) {
        val exportLine = exports.joinToString(", ")
        val file = File(sourceCodeFile)
        val fileText = file.readText()
        when {
            fileText.contains("$exportsLinePrefix{$exportLine}") -> {}
            fileText.contains(startComment) && !fileText.contains("$exportsLinePrefix{$exportLine}") -> {
                val regex = Regex("\n$startComment\n(.*)\n$endComment")
                regex.find(fileText)?.groups?.get(1)?.value?.let {
                    val exportsRegex = Regex("\\{(.*)}")
                    val existingExportsLine = exportsRegex.find(it)!!.groupValues[1]
                    val existingExportsSet = existingExportsLine.filterNot { c -> c == ' ' }.split(',').toMutableSet()
                    existingExportsSet.addAll(exports)
                    val resLine = existingExportsSet.joinToString()
                    val swappedText = fileText.replace(it, "$exportsLinePrefix{$resLine}")
                    file.writeText(swappedText)
                }
            }

            else -> {
                val line = buildString {
                    append("\n$startComment")
                    append("\n$exportsLinePrefix{$exportLine}")
                    append("\n$endComment")
                }
                file.appendText(line)
            }
        }
    }
}