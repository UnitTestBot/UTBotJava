package org.utbot.intellij.plugin.sarif

import com.intellij.openapi.progress.ProgressIndicator
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import com.intellij.openapi.vfs.VfsUtil
import java.util.concurrent.CountDownLatch
import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.intellij.plugin.generator.UtTestsDialogProcessor
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.intellij.plugin.util.IntelliJApiHelper

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
        sourceFinding: SourceFindingStrategyIdea,
        reportsCountDown: CountDownLatch,
        indicator: ProgressIndicator
    ) {
        UtTestsDialogProcessor.updateIndicator(indicator, UtTestsDialogProcessor.ProgressRange.SARIF, "Generate SARIF report for ${classId.name}", .5)
        // building the path to the report file
        val classFqn = classId.name
        val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
        val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(classFqn)}Report.sarif")

        // creating report related directory
        VfsUtil.createDirectoryIfMissing(reportFilePath.parent.toString())

        IntelliJApiHelper.run(IntelliJApiHelper.Target.THREAD_POOL, indicator) {
            try {
                proc.writeSarif(reportFilePath, testSetsId, generatedTestsCode, sourceFinding)
            } catch (e: Exception) {
                logger.error { e }
            } finally {
                reportsCountDown.countDown()
            }
        }
    }
}

