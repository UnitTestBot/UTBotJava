package org.utbot.intellij.plugin.sarif

import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import org.utbot.sarif.SarifReport
import com.intellij.openapi.vfs.VfsUtil
import org.utbot.intellij.plugin.models.GenerateTestsModel

object SarifReportIdea {

    /**
     * Creates the SARIF report by calling the SarifReport.createReport(),
     * saves it to test resources directory and notifies the user about the creation.
     */
    fun createAndSave(
        model: GenerateTestsModel,
        testCases: List<UtTestCase>,
        generatedTestsCode: String,
        sourceFinding: SourceFindingStrategyIdea
    ) {
        // building the path to the report file
        val classFqn = testCases.firstOrNull()?.method?.clazz?.qualifiedName ?: return
        val sarifReportsPath = model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot)
        val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(classFqn)}Report.sarif")

        // creating report related directory
        VfsUtil.createDirectoryIfMissing(reportFilePath.parent.toString())

        // creating & saving sarif report
        reportFilePath
            .toFile()
            .writeText(SarifReport(testCases, generatedTestsCode, sourceFinding).createReport())
    }
}

