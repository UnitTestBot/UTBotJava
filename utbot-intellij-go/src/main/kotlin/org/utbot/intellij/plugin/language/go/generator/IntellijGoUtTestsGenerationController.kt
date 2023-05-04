package org.utbot.intellij.plugin.language.go.generator

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunctionTestCase
import org.utbot.go.gocodeanalyzer.GoSourceCodeAnalyzer
import org.utbot.go.logic.AbstractGoUtTestsGenerationController
import org.utbot.intellij.plugin.language.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.showWarningDialogLater
import java.nio.file.Path

class IntellijGoUtTestsGenerationController(
    private val model: GenerateGoTestsModel,
    private val indicator: ProgressIndicator
) : AbstractGoUtTestsGenerationController() {

    private object ProgressIndicatorConstants {
        const val START_FRACTION = 0.05 // is needed to prevent infinite indicator that appears for 0.0
        const val SOURCE_CODE_ANALYSIS_FRACTION = 0.25
        const val TEST_CASES_CODE_GENERATION_FRACTION = 0.1
        const val TEST_CASES_GENERATION_FRACTION =
            1.0 - SOURCE_CODE_ANALYSIS_FRACTION - TEST_CASES_CODE_GENERATION_FRACTION
    }

    private lateinit var testCasesGenerationCounter: ProcessedFilesCounter
    private lateinit var testCasesCodeGenerationCounter: ProcessedFilesCounter

    private data class ProcessedFilesCounter(
        private val toProcessTotalFilesNumber: Int,
        private var processedFiles: Int = 0
    ) {
        val processedFilesRatio: Double get() = processedFiles.toDouble() / toProcessTotalFilesNumber
        fun addProcessedFile() {
            processedFiles++
        }
    }

    override fun onSourceCodeAnalysisStart(targetFunctionsNamesBySourceFiles: Map<Path, List<String>>): Boolean {
        indicator.isIndeterminate = false
        indicator.text = "Analyze source files"
        indicator.fraction = ProgressIndicatorConstants.START_FRACTION
        return true
    }

    override fun onSourceCodeAnalysisFinished(
        analysisResults: Map<GoUtFile, GoSourceCodeAnalyzer.GoSourceFileAnalysisResult>
    ): Boolean {
        indicator.fraction = indicator.fraction.coerceAtLeast(
            ProgressIndicatorConstants.START_FRACTION + ProgressIndicatorConstants.SOURCE_CODE_ANALYSIS_FRACTION
        )
        if (!handleMissingSelectedFunctions(analysisResults)) return false

        val filesToProcessTotalNumber =
            analysisResults.count { (_, analysisResult) -> analysisResult.functions.isNotEmpty() }
        testCasesGenerationCounter = ProcessedFilesCounter(filesToProcessTotalNumber)
        testCasesCodeGenerationCounter = ProcessedFilesCounter(filesToProcessTotalNumber)
        return true
    }

    override fun onPackageInstrumentationStart(): Boolean {
        return true
    }

    override fun onPackageInstrumentationFinished(): Boolean {
        return true
    }

    override fun onTestCasesGenerationForGoSourceFileFunctionsStart(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>
    ): Boolean {
        indicator.text = "Generate test cases for ${sourceFile.fileName}"
        indicator.fraction = indicator.fraction.coerceAtLeast(
            ProgressIndicatorConstants.START_FRACTION
                    + ProgressIndicatorConstants.SOURCE_CODE_ANALYSIS_FRACTION
                    + ProgressIndicatorConstants.TEST_CASES_GENERATION_FRACTION
                    * testCasesGenerationCounter.processedFilesRatio
        )
        indicator.checkCanceled() // allow user to cancel possibly slow unit test generation
        return true
    }

    override fun onTestCasesGenerationForGoSourceFileFunctionsFinished(
        sourceFile: GoUtFile,
        testCases: List<GoUtFuzzedFunctionTestCase>
    ): Boolean {
        testCasesGenerationCounter.addProcessedFile()
        return true
    }

    override fun onTestCasesFileCodeGenerationStart(
        sourceFile: GoUtFile,
        testCases: List<GoUtFuzzedFunctionTestCase>
    ): Boolean {
        indicator.text = "Generate tests code for ${sourceFile.fileName}"
        indicator.fraction = indicator.fraction.coerceAtLeast(
            ProgressIndicatorConstants.START_FRACTION
                    + ProgressIndicatorConstants.SOURCE_CODE_ANALYSIS_FRACTION
                    + ProgressIndicatorConstants.TEST_CASES_GENERATION_FRACTION
                    + ProgressIndicatorConstants.TEST_CASES_CODE_GENERATION_FRACTION
                    * testCasesCodeGenerationCounter.processedFilesRatio
        )
        return true
    }

    override fun onTestCasesFileCodeGenerationFinished(
        sourceFile: GoUtFile,
        generatedTestsFileCode: String
    ): Boolean {
        invokeLater {
            GoUtTestsCodeFileWriter.createTestsFileWithGeneratedCode(model, sourceFile, generatedTestsFileCode)
        }
        testCasesCodeGenerationCounter.addProcessedFile()
        return true
    }

    private fun handleMissingSelectedFunctions(
        analysisResults: Map<GoUtFile, GoSourceCodeAnalyzer.GoSourceFileAnalysisResult>
    ): Boolean {
        val missingSelectedFunctionsListMessage = generateMissingSelectedFunctionsListMessage(analysisResults)
        val okSelectedFunctionsArePresent =
            analysisResults.any { (_, analysisResult) -> analysisResult.functions.isNotEmpty() }

        if (missingSelectedFunctionsListMessage == null) {
            return okSelectedFunctionsArePresent
        }

        val errorMessageSb = StringBuilder()
            .append("Some selected functions were skipped during source code analysis.")
            .append(missingSelectedFunctionsListMessage)
        if (okSelectedFunctionsArePresent) {
            showWarningDialogLater(
                model.project,
                errorMessageSb.append("Unit test generation for other selected functions will be performed as usual.")
                    .toString(),
                title = "Skipped some functions for unit tests generation"
            )
        } else {
            showErrorDialogLater(
                model.project,
                errorMessageSb.append("Unit test generation is cancelled: no other selected functions.").toString(),
                title = "Unit tests generation is cancelled"
            )
        }
        return okSelectedFunctionsArePresent
    }
}