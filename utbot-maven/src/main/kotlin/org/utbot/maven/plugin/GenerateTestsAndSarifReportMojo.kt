package org.utbot.maven.plugin

import mu.KotlinLogging
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.utbot.common.measureTime
import org.utbot.common.debug
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.sarif.GenerateTestsAndSarifReportFacade
import org.utbot.framework.plugin.sarif.TargetClassWrapper
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.maven.plugin.extension.SarifMavenConfigurationProvider
import org.utbot.maven.plugin.wrappers.MavenProjectWrapper
import org.utbot.maven.plugin.wrappers.SourceFindingStrategyMaven
import java.io.File
import java.net.URLClassLoader

internal val logger = KotlinLogging.logger {}

/**
 * The main class containing the entry point [execute].
 *
 * [Documentation](https://maven.apache.org/guides/plugin/guide-java-plugin-development.html)
 */
@Mojo(
    name = "generateTestsAndSarifReport",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES
)
@Execute(
    phase = LifecyclePhase.GENERATE_TEST_SOURCES
)
class GenerateTestsAndSarifReportMojo : AbstractMojo() {

    /**
     * The maven project for which we are creating a SARIF report.
     */
    @Parameter(defaultValue = "\${project}", readonly = true)
    lateinit var mavenProject: MavenProject

    /**
     * Classes for which the SARIF report will be created.
     * Uses all classes from the user project if this list is empty.
     */
    @Parameter(defaultValue = "")
    internal var targetClasses: List<String> = listOf()

    /**
     * Absolute path to the root of the relative paths in the SARIF report.
     */
    @Parameter(defaultValue = "\${project.basedir}")
    internal lateinit var projectRoot: File

    /**
     * Relative path (against module root) to the root of the generated tests.
     */
    @Parameter(defaultValue = "target/generated/test")
    internal lateinit var generatedTestsRelativeRoot: String

    /**
     * Relative path (against module root) to the root of the SARIF reports.
     */
    @Parameter(defaultValue = "target/generated/sarif")
    internal lateinit var sarifReportsRelativeRoot: String

    /**
     * Mark the directory with generated tests as `test sources root` or not.
     */
    @Parameter(defaultValue = "true")
    internal var markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean = true

    /**
     * Generate tests for private methods or not.
     */
    @Parameter(defaultValue = "false")
    internal var testPrivateMethods: Boolean = false

    /**
     * Can be one of: 'junit4', 'junit5', 'testng'.
     */
    @Parameter(defaultValue = "junit5")
    internal lateinit var testFramework: String

    /**
     * Can be one of: 'mockito'.
     */
    @Parameter(defaultValue = "mockito")
    internal lateinit var mockFramework: String

    /**
     * Maximum tests generation time for one class (in milliseconds).
     */
    @Parameter
    internal var generationTimeout: Long = 60 * 1000L

    /**
     * Can be one of: 'java', 'kotlin'.
     */
    @Parameter(defaultValue = "java")
    internal lateinit var codegenLanguage: String

    /**
     * Can be one of: 'no-mocks', 'other-packages', 'other-classes'.
     */
    @Parameter(defaultValue = "no-mocks")
    internal lateinit var mockStrategy: String

    /**
     * Can be one of: 'do-not-mock-statics', 'mock-statics'.
     */
    @Parameter(defaultValue = "do-not-mock-statics")
    internal lateinit var staticsMocking: String

    /**
     * Can be one of: 'force', 'do-not-force'.
     */
    @Parameter(defaultValue = "force")
    internal lateinit var forceStaticMocking: String

    /**
     * Classes to force mocking theirs static methods and constructors.
     */
    @Parameter(defaultValue = "")
    internal var classesToMockAlways: List<String> = listOf()

    /**
     * Provides configuration needed to create a SARIF report.
     */
    val sarifProperties: SarifMavenConfigurationProvider
        get() = SarifMavenConfigurationProvider(this)

    /**
     * Contains information about the maven project for which we are creating a SARIF report.
     */
    lateinit var rootMavenProjectWrapper: MavenProjectWrapper

    private val dependencyPaths by lazy {
        val thisClassLoader = this::class.java.classLoader as? URLClassLoader
            ?: return@lazy System.getProperty("java.class.path")
        thisClassLoader.urLs.joinToString(File.pathSeparator) { it.path }
    }

    /**
     * Entry point: called when the user starts this maven task.
     */
    override fun execute() {
        try {
            rootMavenProjectWrapper = MavenProjectWrapper(mavenProject, sarifProperties)
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while configuring the maven task" }
            return
        }
        try {
            generateForProjectRecursively(rootMavenProjectWrapper)
            GenerateTestsAndSarifReportFacade.mergeReports(
                sarifReports = rootMavenProjectWrapper.collectReportsRecursively(),
                mergedSarifReportFile = rootMavenProjectWrapper.sarifReportFile
            )
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
        logger.debug().measureTime({ "Generating tests for the '${mavenProjectWrapper.mavenProject.name}' source set" }) {
            withUtContext(UtContext(mavenProjectWrapper.classLoader)) {
                val testCaseGenerator =
                    TestCaseGenerator(
                        listOf(mavenProjectWrapper.workingDirectory),
                        mavenProjectWrapper.runtimeClasspath,
                        dependencyPaths,
                        JdkInfoService.provide()
                    )
                mavenProjectWrapper.targetClasses.forEach { targetClass ->
                    generateForClass(mavenProjectWrapper, targetClass, testCaseGenerator)
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
    private fun generateForClass(
        mavenProjectWrapper: MavenProjectWrapper,
        targetClass: TargetClassWrapper,
        testCaseGenerator: TestCaseGenerator,
    ) {
        logger.debug().measureTime({ "Generating tests for the $targetClass" }) {
            val sourceFindingStrategy =
                SourceFindingStrategyMaven(mavenProjectWrapper, targetClass.testsCodeFile.path)
            val generateTestsAndSarifReportFacade =
                GenerateTestsAndSarifReportFacade(sarifProperties, sourceFindingStrategy, testCaseGenerator)
            generateTestsAndSarifReportFacade
                .generateForClass(targetClass, mavenProjectWrapper.workingDirectory)
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
}
