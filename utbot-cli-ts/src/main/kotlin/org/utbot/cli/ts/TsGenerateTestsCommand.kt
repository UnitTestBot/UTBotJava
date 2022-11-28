package org.utbot.cli.ts

import api.TsTestGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KotlinLogging
import service.TsCoverageMode
import settings.TsDynamicSettings
import settings.TsTestGenerationSettings
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

val logger = KotlinLogging.logger {}

class TsGenerateTestsCommand :
    CliktCommand(name = "generate_ts", help = "Generates tests for the specified class or toplevel functions.") {

    init {
        context {
            valueSources(
                JsonValueSource.from(File(System.getProperty("user.dir") + "/tsconfig.json")),
            )
        }
    }

    private val sourceCodeFile by option(
        "-s", "--source",
        help = "Specifies source code file for test generation."
    )
        .required()
        .check("Must exist and ends with .ts suffix") {
            it.endsWith(".ts") && Files.exists(Paths.get(it))
        }

    private val targetClass: String? by option("-c", "--class", help = "Specifies target class to generate tests for.")

    private val targetFunction: String?
        by option("-f", "--function", help = "Specifies target top-level function to generate tests for.")

    private val output by option("-o", "--output", help = "Specifies output file for generated tests.")
        .check("Must end with .ts suffix") {
            it.endsWith(".ts")
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
    ).default("${TsTestGenerationSettings.defaultTimeout}")

    private val coverageMode by option(
        "--coverage-mode",
        help = "Specifies the coverage mode for test generation. Check docs for more info."
    ).choice(
        TsCoverageMode.BASIC.toString() to TsCoverageMode.BASIC,
        TsCoverageMode.FAST.toString() to TsCoverageMode.FAST
    ).default(TsCoverageMode.FAST)

    private val pathToTsNode by option(
        "--path-to-ts-node",
        help = "Sets path to ts-node executable, defaults to \"ts-node\" shortcut. " +
                "As there are many nyc files in the global npm directory, choose one without file extension."
    ).default("ts-node")

    private val pathToNYC by option(
        "--path-to-nyc",
        help = "Sets path to nyc executable, defaults to \"nyc\" shortcut. " +
                "As there are many nyc files in the global npm directory, choose one without file extension."
    ).default("nyc")

    private val pathToNycTs by option(
        "--path-to-nyc-cfg-ts",
        help = "Sets path to nyc-config-typescript module\n" +
                "Install it using \"npm i @istanbuljs/nyc-config-typescript\" command in your project. " +
                "It will be located in \"node_modules/@istanbuljs\" folder."
    ).required()

    private val pathToTsModule by option(
        "--path-to-ts-module",
        help = "Sets path to \"typescript\" module in \"node_modules\" folder"
    ).required()


    private val projectPath by option(
        "--project-path",
        help = "Sets path to the project containing file under test."
    ).required()

    private val godObjectClass by option(
        "-g",
        "--god-object",
        help = "Specifies class that will be taken as god object for test generation"
    )

    override fun run() {
        /*
            targetClass and targetFunction can't be specified at the same time.
        */
        if (targetClass != null && targetFunction != null) {
            logger.error { "\"--class\" and \"--function\" options can't be specified at the same time!" }
            return
        }
        val started = LocalDateTime.now()
        try {
            val sourceFileAbsolutePath = TsUtils.makeAbsolutePath(sourceCodeFile)
            logger.info { "Generating tests for [$sourceFileAbsolutePath] - started" }
            val outputAbsolutePath = output?.let { TsUtils.makeAbsolutePath(it) }
            val testGenerator = TsTestGenerator(
                sourceFilePath = sourceFileAbsolutePath,
                projectPath = TsUtils.makeAbsolutePath(projectPath),
                selectedMethods = targetFunction?.let { listOf(it) },
                parentClassName = targetClass,
                outputFilePath = outputAbsolutePath,
                settings = TsDynamicSettings(
                    pathToNYC = pathToNYC,
                    timeout = timeout.toLong(),
                    coverageMode = coverageMode,
                    tsNycModulePath = pathToNycTs,
                    tsNodePath = pathToTsNode,
                    tsModulePath = pathToTsModule,
                    godObject = godObjectClass
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
}
