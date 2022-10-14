package org.utbot.intellij.plugin.sarif

import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiClass
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.framework.plugin.api.ClassId
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.process.EngineProcess
import org.utbot.intellij.plugin.ui.utils.getOrCreateSarifReportsPath
import java.nio.file.Path

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
        psiClass: PsiClass
    ) {
        // building the path to the report file
        val classFqn = classId.name
        val (sarifReportsPath, sourceFinding) = WriteAction.computeAndWait<Pair<Path, SourceFindingStrategyIdea>, Exception> { model.testModule.getOrCreateSarifReportsPath(model.testSourceRoot) to SourceFindingStrategyIdea(psiClass) }
        val reportFilePath = sarifReportsPath.resolve("${classFqnToPath(classFqn)}Report.sarif")

        proc.writeSarif(reportFilePath, testSetsId, generatedTestsCode, sourceFinding)
    }
}

