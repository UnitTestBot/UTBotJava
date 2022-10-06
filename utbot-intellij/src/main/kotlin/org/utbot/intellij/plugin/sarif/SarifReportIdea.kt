package org.utbot.intellij.plugin.sarif

import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.utbot.framework.plugin.api.ClassId
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess

object SarifReportIdea {

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
        sourceFinding: SourceFindingStrategyIdea
    ) {
        // building the path to the report file
        val classFqn = classId.name
        val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
        val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(classFqn)}Report.sarif")

        // creating report related directory
        runWriteAction { VfsUtil.createDirectoryIfMissing(reportFilePath.parent.toString()) }

        proc.writeSarif(reportFilePath, testSetsId, generatedTestsCode, sourceFinding)
    }
}

