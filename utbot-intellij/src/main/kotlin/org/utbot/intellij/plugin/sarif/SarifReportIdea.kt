package org.utbot.intellij.plugin.sarif

import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.utbot.common.HTML_LINE_SEPARATOR
import org.utbot.common.PathUtil
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.intellij.plugin.generator.CodeGenerationController
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.intellij.plugin.ui.SarifReportNotifier
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.sarif.Sarif
import org.utbot.sarif.SarifReport
import org.utbot.sarif.SourceFindingStrategyDefault
import java.nio.file.Path

object SarifReportIdea {

    /**
     * Creates the SARIF report by calling the SarifReport.createReport(),
     * saves it to test resources directory and notifies the user about the creation.
     */
    fun createAndSave(
        proc: EngineProcess,
        testSetsId: Long,
        srcClassFqn: String,
        model: GenerateTestsModel,
        testClassPointer: SmartPsiElementPointer<PsiClass>,
        generatedTestsCode: String,
    ): Sarif {
        var resultSarif = Sarif.empty()
        try {
            executeCommand(model.project, "Saving SARIF report") {
                // building the path to the report file
                val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
                val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(srcClassFqn)}Report.sarif")

                // creating report related directory
                runWriteAction { VfsUtil.createDirectoryIfMissing(reportFilePath.parent.toString()) }

                // creating & saving the report
                val sourceFinding = SourceFindingStrategyDefault(
                    "io.github.ideaseeker.Main",
                    "C:/Users/sWX1137517/IdeaProjects/SarifTest/src/main/java/io/github/ideaseeker/Main.java",
                    "C:/Users/sWX1137517/IdeaProjects/SarifTest/src/test/java/io/github/ideaseeker/MainTest.java",
                    "C:/Users/sWX1137517/IdeaProjects/SarifTest/",
                ) //SourceFindingStrategyIdea(testClassPointer)
                resultSarif = proc.writeSarif(reportFilePath, testSetsId, generatedTestsCode, sourceFinding)
            }
        } catch (e: Exception) {
            CodeGenerationController.logger.error { e }
            showErrorDialogLater(
                model.project,
                message = "Cannot save Sarif report via generated tests: error occurred '${e.message}'",
                title = "Failed to save Sarif report"
            )
        }
        return resultSarif
    }

    /**
     * Merges all SARIF reports into one large containing all the information.
     * Saves it to the [sarifReportsPath] and notifies the user.
     */
    fun mergeSarifReports(model: GenerateTestsModel, sarifReportsPath: Path) {
        val mergedReportFile = sarifReportsPath
            .resolve("${model.project.name}Report.sarif")
            .toFile()
        // deleting the old report so that `sarifReports` does not contain it
        mergedReportFile.delete()

        val sarifReports = sarifReportsPath.toFile()
            .walkTopDown()
            .filter { it.extension == "sarif" }
            .map { it.readText() }
            .toList()

        val mergedReport = SarifReport.mergeReports(sarifReports)
        mergedReportFile.writeText(mergedReport)

        // notifying the user
        SarifReportNotifier.notify(
            info = """
                SARIF report was saved to ${PathUtil.toHtmlLinkTag(mergedReportFile.path)}$HTML_LINE_SEPARATOR
            """.trimIndent()
        )
    }
}

