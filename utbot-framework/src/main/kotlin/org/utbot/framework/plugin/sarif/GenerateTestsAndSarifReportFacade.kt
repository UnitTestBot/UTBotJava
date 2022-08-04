package org.utbot.framework.plugin.sarif

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.id
import org.utbot.sarif.SarifReport
import org.utbot.sarif.SourceFindingStrategy
import org.utbot.summary.summarize
import java.io.File
import java.nio.file.Path

/**
 * Facade for `generateTestsAndSarifReport` task/mojo.
 * Stores common logic between gradle and maven plugins.
 */
class GenerateTestsAndSarifReportFacade(
    private val sarifProperties: SarifExtensionProvider,
    private val sourceFindingStrategy: SourceFindingStrategy,
    private val testCaseGenerator: TestCaseGenerator,
) {
    /**
     * Generates tests and a SARIF report for the class [targetClass].
     * Requires withUtContext() { ... }.
     */
    fun generateForClass(targetClass: TargetClassWrapper, workingDirectory: Path) {
        val testSets = generateTestSets(targetClass, workingDirectory)
        val testClassBody = generateTestCode(targetClass, testSets)
        targetClass.testsCodeFile.writeText(testClassBody)

        generateReport(targetClass, testSets, testClassBody, sourceFindingStrategy)
    }

    companion object {
        /**
         * Merges all [sarifReports] into one large [mergedSarifReportFile] containing all the information.
         * Prints a message about where the SARIF file is saved if [verbose] is true.
         */
        fun mergeReports(
            sarifReports: List<String>,
            mergedSarifReportFile: File,
            verbose: Boolean = true
        ) {
            val mergedReport = SarifReport.mergeReports(sarifReports)
            mergedSarifReportFile.writeText(mergedReport)
            if (verbose) {
                println("SARIF report was saved to \"${mergedSarifReportFile.path}\"")
            }
        }
    }

    private fun generateTestSets(targetClass: TargetClassWrapper, workingDirectory: Path): List<UtMethodTestSet> =
        testCaseGenerator
            .generate(
                targetClass.targetMethods,
                sarifProperties.mockStrategy,
                sarifProperties.classesToMockAlways,
                sarifProperties.generationTimeout
            ).map {
                it.summarize(targetClass.sourceCodeFile, workingDirectory)
            }

    private fun generateTestCode(targetClass: TargetClassWrapper, testSets: List<UtMethodTestSet>): String =
        initializeCodeGenerator(targetClass)
            .generateAsString(testSets, targetClass.testsCodeFile.nameWithoutExtension)

    private fun initializeCodeGenerator(targetClass: TargetClassWrapper): CodeGenerator {
        val isNoStaticMocking = sarifProperties.staticsMocking is NoStaticMocking
        val isForceStaticMocking = sarifProperties.forceStaticMocking == ForceStaticMocking.FORCE

        return CodeGenerator(
            classUnderTest = targetClass.classUnderTest.id,
            testFramework = sarifProperties.testFramework,
            mockFramework = sarifProperties.mockFramework,
            staticsMocking = sarifProperties.staticsMocking,
            forceStaticMocking = sarifProperties.forceStaticMocking,
            generateWarningsForStaticMocking = isNoStaticMocking && isForceStaticMocking,
            codegenLanguage = sarifProperties.codegenLanguage
        )
    }

    /**
     * Creates a SARIF report for the class [targetClass].
     * Saves the report to the file specified in [targetClass].
     */
    private fun generateReport(
        targetClass: TargetClassWrapper,
        testSets: List<UtMethodTestSet>,
        testClassBody: String,
        sourceFinding: SourceFindingStrategy
    ) {
        val sarifReport = SarifReport(testSets, testClassBody, sourceFinding).createReport()
        targetClass.sarifReportFile.writeText(sarifReport)
    }
}