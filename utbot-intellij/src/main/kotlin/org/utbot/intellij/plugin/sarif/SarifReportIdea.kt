package org.utbot.intellij.plugin.sarif

import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiClass
import com.intellij.openapi.progress.ProgressIndicator
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import java.util.concurrent.CountDownLatch
import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.intellij.plugin.generator.UtTestsDialogProcessor
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.sarif.Sarif
import org.utbot.intellij.plugin.util.IntelliJApiHelper
import java.nio.file.Path

object SarifReportIdea {
    private val logger = KotlinLogging.logger {}
    /**
     * Creates the SARIF report by calling the SarifReport.createReport(),
     * saves it to test resources directory and notifies the user about the creation.
     */
    fun createAndSave(
        proc: EngineProcess,
        testSetsId: Long,
        classId: ClassId,
        model: GenerateTestsModel,
        generatedTestsCode: String,
        psiClass: PsiClass,
        reportsCountDown: CountDownLatch,
        srcClassPathToSarifReport: MutableMap<Path, Sarif>,
        srcClassPath: Path,
        indicator: ProgressIndicator
    ) {
        UtTestsDialogProcessor.updateIndicator(indicator, UtTestsDialogProcessor.ProgressRange.SARIF, "Generate SARIF report for ${classId.name}", .5)
        // building the path to the report file
        val classFqn = classId.name
        val (sarifReportsPath, sourceFinding) = WriteAction.computeAndWait<Pair<Path, SourceFindingStrategyIdea>, Exception> {
            model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot) to SourceFindingStrategyIdea(psiClass)
        }
        val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(classFqn)}Report.sarif")

        IntelliJApiHelper.run(IntelliJApiHelper.Target.THREAD_POOL, indicator, "Save SARIF report for ${classId.name}") {
            try {
                val sarifReportAsJson = proc.writeSarif(reportFilePath, testSetsId, generatedTestsCode, sourceFinding)
                val newSarifReport = Sarif.fromJson(sarifReportAsJson)
                val oldSarifReport = srcClassPathToSarifReport[srcClassPath] ?: Sarif.empty()
                srcClassPathToSarifReport[srcClassPath] = oldSarifReport + newSarifReport
            } catch (e: Exception) {
                logger.error { e }
            } finally {
                reportsCountDown.countDown()
            }
        }
    }
}

