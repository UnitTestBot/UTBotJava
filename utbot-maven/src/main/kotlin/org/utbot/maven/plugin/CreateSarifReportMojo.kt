package org.utbot.maven.plugin

import mu.KotlinLogging
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.utbot.common.bracket
import org.utbot.common.debug
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.model.ModelBasedTestCodeGenerator
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.gradle.plugin.wrappers.TargetClassWrapper
import org.utbot.maven.plugin.wrappers.MavenProjectWrapper
import org.utbot.maven.plugin.wrappers.SourceFindingStrategyMaven
import org.utbot.sarif.SarifReport
import org.utbot.summary.summarize
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Mojo(
    name = "createSarifReport",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES
)
@Execute(
    phase = LifecyclePhase.GENERATE_TEST_SOURCES
)
class CreateSarifReportMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true)
    private lateinit var mavenProject: MavenProject

    @Parameter(defaultValue = "")
    internal var targetClasses: List<String> = listOf()

    @Parameter(defaultValue = "\${project.basedir}")
    internal lateinit var projectRoot: File

    @Parameter(defaultValue = "target/generated/test")
    internal lateinit var generatedTestsRelativeRoot: File

    @Parameter(defaultValue = "target/generated/sarif")
    internal lateinit var sarifReportsRelativeRoot: File

    @Parameter(defaultValue = "true")
    internal var markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean = true

    @Parameter(defaultValue = "junit5")
    internal lateinit var testFramework: String

    @Parameter(defaultValue = "mockito")
    internal lateinit var mockFramework: String

    @Parameter
    internal var generationTimeout: Long = 60 * 1000L

    @Parameter(defaultValue = "java")
    internal lateinit var codegenLanguage: String

    @Parameter(defaultValue = "do-not-mock")
    internal lateinit var mockStrategy: String

    @Parameter(defaultValue = "do-not-mock-statics")
    internal lateinit var staticsMocking: String

    @Parameter(defaultValue = "force")
    internal lateinit var forceStaticMocking: String

    @Parameter(defaultValue = "")
    internal var classesToMockAlways: List<String> = listOf()

    private val sarifProperties = SarifMavenConfigurationProvider(this)

    /**
     * Entry point: called when the user starts this maven task.
     */
    override fun execute() {
        val rootMavenProjectWrapper = try {
            MavenProjectWrapper(mavenProject, sarifProperties)
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while configuring the maven task" }
            return
        }
        try {
            generateForProjectRecursively(rootMavenProjectWrapper)
            mergeReports(rootMavenProjectWrapper)
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while generating SARIF report" }
            return
        }
    }

    // internal

    /**
     * Generates tests and a SARIF report for classes in the [mavenProjectWrapper] and in all its child projects.
     */
    private fun generateForProjectRecursively(mavenProjectWrapper: MavenProjectWrapper) {
        logger.debug().bracket("Generating tests for the '${mavenProjectWrapper.mavenProject.name}' source set") {
            withUtContext(UtContext(mavenProjectWrapper.classLoader)) {
                mavenProjectWrapper.targetClasses.forEach { targetClass ->
                    generateForClass(mavenProjectWrapper, targetClass)
                }
            }
        }
        mavenProjectWrapper.childProjects.forEach { childProject ->
            generateForProjectRecursively(childProject)
        }
    }

    /**
     * Generates tests and a SARIF report for the class [targetClass].
     */
    private fun generateForClass(mavenProjectWrapper: MavenProjectWrapper, targetClass: TargetClassWrapper) {
        logger.debug().bracket("Generating tests for the $targetClass") {
            initializeEngine(mavenProjectWrapper.runtimeClasspath, mavenProjectWrapper.workingDirectory)

            val testCases = generateTestCases(targetClass, mavenProjectWrapper.workingDirectory)
            val testClassBody = generateTestCode(targetClass, testCases)
            targetClass.testsCodeFile.writeText(testClassBody)

            generateReport(mavenProjectWrapper, targetClass, testCases, testClassBody)
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
        mavenProjectWrapper: MavenProjectWrapper,
        targetClass: TargetClassWrapper,
        testCases: List<UtTestCase>,
        testClassBody: String
    ) {
        logger.debug().bracket("Creating a SARIF report for the $targetClass") {
            val sourceFinding = SourceFindingStrategyMaven(mavenProjectWrapper, targetClass.testsCodeFile.path)
            val sarifReport = SarifReport(testCases, testClassBody, sourceFinding).createReport()
            targetClass.sarifReportFile.writeText(sarifReport)
        }
    }

    /**
     * Returns SARIF reports created for this [MavenProjectWrapper] and for all its child projects.
     */
    private fun MavenProjectWrapper.collectReportsRecursively(): List<String> =
        this.childProjects.flatMap { childProject ->
            childProject.collectReportsRecursively()
        } + this.collectReports()

    /**
     * Returns SARIF reports created for this [MavenProjectWrapper].
     */
    private fun MavenProjectWrapper.collectReports(): List<String> =
        this.targetClasses.map { targetClass ->
            targetClass.sarifReportFile.readText()
        }

    /**
     * Merges all SARIF reports into one large containing all the information.
     */
    private fun mergeReports(mavenProjectWrapper: MavenProjectWrapper) {
        val reports = mavenProjectWrapper.collectReportsRecursively()
        val mergedReport = SarifReport.mergeReports(reports)
        mavenProjectWrapper.sarifReportFile.writeText(mergedReport)
        println("SARIF report was saved to \"${mavenProjectWrapper.sarifReportFile.path}\"")
        println("You can open it using the VS Code extension \"Sarif Viewer\"")
    }
}
