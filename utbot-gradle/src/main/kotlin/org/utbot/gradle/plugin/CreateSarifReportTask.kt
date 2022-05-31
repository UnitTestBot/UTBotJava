package org.utbot.gradle.plugin

import mu.KLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.utbot.common.bracket
import org.utbot.common.debug
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.model.ModelBasedTestCodeGenerator
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import org.utbot.gradle.plugin.wrappers.GradleProjectWrapper
import org.utbot.gradle.plugin.wrappers.SourceFindingStrategyGradle
import org.utbot.gradle.plugin.wrappers.SourceSetWrapper
import org.utbot.gradle.plugin.wrappers.TargetClassWrapper
import org.utbot.sarif.SarifReport
import org.utbot.summary.summarize
import java.net.URLClassLoader
import java.nio.file.Path
import javax.inject.Inject

/**
 * The main class containing the entry point [createSarifReport].
 *
 * [Documentation](https://docs.gradle.org/current/userguide/custom_tasks.html)
 */
open class CreateSarifReportTask @Inject constructor(
    private val sarifProperties: SarifGradleExtensionProvider
) : DefaultTask() {

    init {
        group = "utbot"
        description = "Generate a SARIF report"
    }

    /**
     * Entry point: called when the user starts this gradle task.
     */
    @TaskAction
    fun createSarifReport() {
        val rootGradleProject = try {
            GradleProjectWrapper(project, sarifProperties)
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while configuring the gradle task" }
            return
        }
        try {
            generateForProjectRecursively(rootGradleProject)
            mergeReports(rootGradleProject)
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while generating SARIF report" }
            return
        }
    }

    // internal

    // overwriting the getLogger() function from the DefaultTask
    private val logger: KLogger = org.utbot.gradle.plugin.logger

    /**
     * Generates tests and a SARIF report for classes in the [gradleProject] and in all its child projects.
     */
    private fun generateForProjectRecursively(gradleProject: GradleProjectWrapper) {
        gradleProject.sourceSets.forEach { sourceSet ->
            generateForSourceSet(sourceSet)
        }
        gradleProject.childProjects.forEach { childProject ->
            generateForProjectRecursively(childProject)
        }
    }

    /**
     * Generates tests and a SARIF report for classes in the [sourceSet].
     */
    private fun generateForSourceSet(sourceSet: SourceSetWrapper) {
        logger.debug().bracket("Generating tests for the '${sourceSet.sourceSet.name}' source set") {
            // UtContext is used in `generateTestCases` and `generateTestCode`
            withUtContext(UtContext(sourceSet.classLoader)) {
                sourceSet.targetClasses.forEach { targetClass ->
                    generateForClass(sourceSet, targetClass)
                }
            }
        }
    }

    /**
     * Generates tests and a SARIF report for the class [targetClass].
     */
    private fun generateForClass(sourceSet: SourceSetWrapper, targetClass: TargetClassWrapper) {
        logger.debug().bracket("Generating tests for the $targetClass") {
            initializeEngine(sourceSet.runtimeClasspath, sourceSet.workingDirectory)

            val testCases = generateTestCases(targetClass, sourceSet.workingDirectory)
            val testClassBody = generateTestCode(targetClass, testCases)
            targetClass.testsCodeFile.writeText(testClassBody)

            generateReport(sourceSet, targetClass, testCases, testClassBody)
        }
    }

    private val dependencyPaths by lazy {
        val thisClassLoader = this::class.java.classLoader as URLClassLoader
        thisClassLoader.urLs.joinToString(separator = ";") { it.path }
    }

    private fun initializeEngine(classPath: String, workingDirectory: Path) {
        UtBotTestCaseGenerator.init(workingDirectory, classPath, dependencyPaths) { false }
    }

    private fun generateTestCases(targetClass: TargetClassWrapper, workingDirectory: Path): List<UtTestCase> =
        UtBotTestCaseGenerator.generateForSeveralMethods(
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
        ModelBasedTestCodeGenerator().apply {
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

    // SARIF reports

    /**
     * Creates a SARIF report for the class [targetClass].
     * Saves the report to the file specified in [targetClass].
     */
    private fun generateReport(
        sourceSet: SourceSetWrapper,
        targetClass: TargetClassWrapper,
        testCases: List<UtTestCase>,
        testClassBody: String
    ) {
        logger.debug().bracket("Creating a SARIF report for the $targetClass") {
            val sourceFinding = SourceFindingStrategyGradle(sourceSet, targetClass.testsCodeFile.path)
            val sarifReport = SarifReport(testCases, testClassBody, sourceFinding).createReport()
            targetClass.sarifReportFile.writeText(sarifReport)
        }
    }

    /**
     * Returns SARIF reports created for this [GradleProjectWrapper] and for all its child projects.
     */
    private fun GradleProjectWrapper.collectReportsRecursively(): List<String> =
        this.sourceSets.flatMap { sourceSetWrapper ->
            sourceSetWrapper.collectReports()
        } + this.childProjects.flatMap { childProject ->
            childProject.collectReportsRecursively()
        }

    /**
     * Returns SARIF reports created for this [SourceSetWrapper].
     */
    private fun SourceSetWrapper.collectReports(): List<String> =
        this.targetClasses.map { targetClass ->
            targetClass.sarifReportFile.readText()
        }

    /**
     * Merges all SARIF reports into one large containing all the information.
     */
    private fun mergeReports(gradleProject: GradleProjectWrapper) {
        val reports = gradleProject.collectReportsRecursively()
        val mergedReport = SarifReport.mergeReports(reports)
        gradleProject.sarifReportFile.writeText(mergedReport)
        println("SARIF report was saved to \"${gradleProject.sarifReportFile.path}\"")
        println("You can open it using the VS Code extension \"Sarif Viewer\"")
    }
}
