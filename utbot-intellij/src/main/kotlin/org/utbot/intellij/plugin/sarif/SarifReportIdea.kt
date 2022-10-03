package org.utbot.intellij.plugin.sarif

import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import com.intellij.openapi.vfs.VfsUtil
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess

object SarifReportIdea {

    /**
     * Creates the SARIF report by calling the SarifReport.createReport(),
     * saves it to test resources directory and notifies the user about the creation.
     */
    fun createAndSave(
        proc: EngineProcess,
        model: GenerateTestsModel,
        testSets: List<UtMethodTestSet>,
        generatedTestsCode: String,
        sourceFinding: SourceFindingStrategyIdea
    ) {
        // building the path to the report file
        val classFqn = testSets.firstOrNull()?.method?.classId?.name ?: return
        val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
        val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(classFqn)}Report.sarif")

        // creating report related directory
        VfsUtil.createDirectoryIfMissing(reportFilePath.parent.toString())

        proc.writeSarif(reportFilePath, testSets, generatedTestsCode, sourceFinding)
    }
}

