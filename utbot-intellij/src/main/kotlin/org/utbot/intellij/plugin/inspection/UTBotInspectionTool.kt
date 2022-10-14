package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.utbot.sarif.Sarif
import org.utbot.sarif.SarifRegion
import org.utbot.sarif.SarifResult
import java.nio.file.Path

/**
 * Global inspection tool that displays detected errors from the SARIF report.
 */
class UTBotInspectionTool : GlobalSimpleInspectionTool() {

    /**
     * Map from the path to the class under test to [Sarif] for it.
     */
    private var srcClassPathToSarifReport: MutableMap<Path, Sarif> = mutableMapOf()

    companion object {
        fun getInstance(srcClassPathToSarifReport: MutableMap<Path, Sarif>) =
            UTBotInspectionTool().also {
                it.srcClassPathToSarifReport = srcClassPathToSarifReport
            }
    }

    override fun getShortName() = "UnitTestBotInspectionTool"

    override fun getDisplayName() = "Unchecked exceptions"

    override fun getGroupDisplayName() = "Errors detected by UnitTestBot"

    /**
     * Appends all the errors from the SARIF report for [psiFile] to the [problemDescriptionsProcessor].
     */
    override fun checkFile(
        psiFile: PsiFile,
        manager: InspectionManager,
        problemsHolder: ProblemsHolder,
        globalContext: GlobalInspectionContext,
        problemDescriptionsProcessor: ProblemDescriptionsProcessor
    ) {
        val sarifReport = srcClassPathToSarifReport[psiFile.virtualFile.toNioPath()]
            ?: return // no results for this file

        for (sarifResult in sarifReport.getAllResults()) {
            val srcFileLocation = sarifResult.locations.firstOrNull()
                ?: continue

            val errorRegion = srcFileLocation.physicalLocation.region
            val errorTextRange = getTextRange(problemsHolder.project, psiFile, errorRegion)

            // see `org.utbot.sarif.SarifReport.processUncheckedException` for the message template
            val (exceptionMessage, testCaseMessage) =
                sarifResult.message.text.split('\n').take(2)
            val sarifResultMessage = "$exceptionMessage $testCaseMessage"

            val testFileLocation = sarifResult.relatedLocations.firstOrNull()?.physicalLocation
            val viewGeneratedTestFix = testFileLocation?.let {
                ViewGeneratedTestFix(
                    testFileRelativePath = it.artifactLocation.uri,
                    lineNumber = it.region.startLine,
                    columnNumber = it.region.startColumn ?: 1
                )
            }

            val stackTraceLines = sarifResult.extractStackTraceLines()
            val analyzeStackTraceFix = AnalyzeStackTraceFix(exceptionMessage, stackTraceLines)

            val problemDescriptor = problemsHolder.manager.createProblemDescriptor(
                psiFile,
                errorTextRange,
                sarifResultMessage,
                ProblemHighlightType.ERROR,
                /* onTheFly = */ true,
                viewGeneratedTestFix,
                analyzeStackTraceFix
            )
            problemDescriptionsProcessor.addProblemElement(
                globalContext.refManager.getReference(psiFile),
                problemDescriptor
            )
        }
    }

    // internal

    /**
     * Converts [SarifRegion] to the [TextRange] of the given [file].
     */
    private fun getTextRange(project: Project, file: PsiFile, region: SarifRegion): TextRange {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file.containingFile)
            ?: return TextRange.EMPTY_RANGE

        val lineNumber = region.startLine - 1 // to 0-based
        val columnNumber = region.startColumn ?: 1

        val lineStartOffset = document.getLineStartOffset(lineNumber) + columnNumber - 1
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        return TextRange(lineStartOffset, lineEndOffset)
    }

    private fun SarifResult.extractStackTraceLines(): List<String> =
        this.codeFlows.flatMap { sarifCodeFlow ->
            sarifCodeFlow.threadFlows.flatMap { sarifThreadFlow ->
                sarifThreadFlow.locations.map { sarifFlowLocationWrapper ->
                    sarifFlowLocationWrapper.location.message.text
                }
            }
        }.reversed()
}
