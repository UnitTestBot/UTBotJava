package org.utbot.framework.plugin.sarif

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.sarif.SarifReport
import org.utbot.sarif.SourceFindingStrategy
import org.utbot.summary.summarize
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Facade for `generateTestsAndSarifReport` task/mojo.
 * Stores common logic between gradle and maven plugins.
 */
class GenerateTestsAndSarifReportFacade(
    val sarifProperties: SarifExtensionProvider,
    val sourceFindingStrategy: SourceFindingStrategy
) {

    /**
     * Generates tests and a SARIF report for the class [targetClass].
     * Requires withUtContext() { ... }.
     */
    fun generateForClass(
        targetClass: TargetClassWrapper,
        workingDirectory: Path,
        runtimeClasspath: String
    ) {
        initializeEngine(runtimeClasspath, workingDirectory)

        val testCases = generateTestCases(targetClass, workingDirectory)
        val testClassBody = generateTestCode(targetClass, testCases)
        targetClass.testsCodeFile.writeText(testClassBody)

        generateReport(targetClass, testCases, testClassBody, sourceFindingStrategy)
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
                println("You can open it using the VS Code extension \"Sarif Viewer\"")
            }
        }
    }

    // internal

    private val dependencyPaths by lazy {
        val thisClassLoader = this::class.java.classLoader as URLClassLoader
        thisClassLoader.urLs.joinToString(File.pathSeparator) { it.path }
    }

    private fun initializeEngine(classPath: String, workingDirectory: Path) {
        TestCaseGenerator.init(workingDirectory, classPath, dependencyPaths)
    }

    private fun generateTestCases(targetClass: TargetClassWrapper, workingDirectory: Path): List<UtTestCase> =
        TestCaseGenerator.generate(
            targetClass.targetMethods(),
            sarifProperties.mockStrategy,
            sarifProperties.classesToMockAlways,
            sarifProperties.generationTimeout
        ).map {
            it.summarize(targetClass.sourceCodeFile, workingDirectory)
        }

    private fun generateTestCode(targetClass: TargetClassWrapper, testCases: List<UtTestCase>): String =
        initializeCodeGenerator(targetClass)
            .generateAsString(testCases, targetClass.testsCodeFile.nameWithoutExtension)

    private fun initializeCodeGenerator(targetClass: TargetClassWrapper) =
        CodeGenerator().apply {
            val isNoStaticMocking = sarifProperties.staticsMocking is NoStaticMocking
            val isForceStaticMocking = sarifProperties.forceStaticMocking == ForceStaticMocking.FORCE

            init(
                classUnderTest = targetClass.classUnderTest.java,
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
        testCases: List<UtTestCase>,
        testClassBody: String,
        sourceFinding: SourceFindingStrategy
    ) {
        val sarifReport = SarifReport(testCases, testClassBody, sourceFinding).createReport()
        targetClass.sarifReportFile.writeText(sarifReport)
    }
}