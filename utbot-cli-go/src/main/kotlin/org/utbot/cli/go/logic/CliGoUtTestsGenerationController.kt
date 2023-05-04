package org.utbot.cli.go.logic

import mu.KotlinLogging
import org.utbot.cli.go.util.durationInMillis
import org.utbot.cli.go.util.now
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunctionTestCase
import org.utbot.go.gocodeanalyzer.GoSourceCodeAnalyzer
import org.utbot.go.logic.AbstractGoUtTestsGenerationController
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

class CliGoUtTestsGenerationController(
    private val printToStdOut: Boolean,
    private val overwriteTestFiles: Boolean
) : AbstractGoUtTestsGenerationController() {

    private lateinit var currentStageStarted: LocalDateTime

    override fun onSourceCodeAnalysisStart(targetFunctionsNamesBySourceFiles: Map<Path, List<String>>): Boolean {
        currentStageStarted = now()
        logger.debug { "Source code analysis - started" }

        return true
    }

    override fun onSourceCodeAnalysisFinished(
        analysisResults: Map<GoUtFile, GoSourceCodeAnalyzer.GoSourceFileAnalysisResult>
    ): Boolean {
        val stageDuration = durationInMillis(currentStageStarted)
        logger.debug { "Source code analysis - completed in [$stageDuration] (ms)" }

        return handleMissingSelectedFunctions(analysisResults)
    }

    override fun onPackageInstrumentationStart(): Boolean {
        currentStageStarted = now()
        logger.debug { "Package instrumentation - started" }

        return true
    }

    override fun onPackageInstrumentationFinished(): Boolean {
        val stageDuration = durationInMillis(currentStageStarted)
        logger.debug { "Package instrumentation - completed in [$stageDuration] (ms)" }

        return true
    }

    override fun onTestCasesGenerationForGoSourceFileFunctionsStart(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>
    ): Boolean {
        currentStageStarted = now()
        logger.debug { "Test cases generation for [${sourceFile.fileName}] - started" }

        return true
    }

    override fun onTestCasesGenerationForGoSourceFileFunctionsFinished(
        sourceFile: GoUtFile,
        testCases: List<GoUtFuzzedFunctionTestCase>
    ): Boolean {
        val stageDuration = durationInMillis(currentStageStarted)
        logger.debug {
            "Test cases generation for [${sourceFile.fileName}] functions - completed in [$stageDuration] (ms)"
        }

        return true
    }

    override fun onTestCasesFileCodeGenerationStart(
        sourceFile: GoUtFile,
        testCases: List<GoUtFuzzedFunctionTestCase>
    ): Boolean {
        currentStageStarted = now()
        logger.debug { "Test cases file code generation for [${sourceFile.fileName}] - started" }

        return true
    }

    override fun onTestCasesFileCodeGenerationFinished(sourceFile: GoUtFile, generatedTestsFileCode: String): Boolean {
        if (printToStdOut) {
            logger.info { generatedTestsFileCode }
            return true
        }
        writeGeneratedCodeToFile(sourceFile, generatedTestsFileCode)

        val stageDuration = durationInMillis(currentStageStarted)
        logger.debug {
            "Test cases file code generation for [${sourceFile.fileName}] functions - completed in [$stageDuration] (ms)"
        }

        return true
    }

    private fun handleMissingSelectedFunctions(
        analysisResults: Map<GoUtFile, GoSourceCodeAnalyzer.GoSourceFileAnalysisResult>
    ): Boolean {
        val missingSelectedFunctionsListMessage = generateMissingSelectedFunctionsListMessage(analysisResults)
        val okSelectedFunctionsArePresent =
            analysisResults.any { (_, analysisResult) -> analysisResult.functions.isNotEmpty() }

        if (missingSelectedFunctionsListMessage != null) {
            logger.warn { "Some selected functions were skipped during source code analysis.$missingSelectedFunctionsListMessage" }
        }
        if (!okSelectedFunctionsArePresent) {
            throw Exception("Nothing to process. No functions were provided")
        }

        return true
    }

    private fun writeGeneratedCodeToFile(sourceFile: GoUtFile, generatedTestsFileCode: String) {
        val testsFileNameWithExtension = createTestsFileNameWithExtension(sourceFile)
        val testFile = File(sourceFile.absoluteDirectoryPath).resolve(testsFileNameWithExtension)
        if (testFile.exists()) {
            val alreadyExistsMessage = "File [${testFile.absolutePath}] already exists"
            if (overwriteTestFiles) {
                logger.warn { "$alreadyExistsMessage: it will be overwritten" }
            } else {
                logger.warn { "$alreadyExistsMessage: skipping test generation for [${sourceFile.fileName}]" }
                return
            }
        }
        testFile.writeText(generatedTestsFileCode)
    }

    private fun createTestsFileNameWithExtension(sourceFile: GoUtFile) =
        sourceFile.fileNameWithoutExtension + "_go_ut_test.go"
}