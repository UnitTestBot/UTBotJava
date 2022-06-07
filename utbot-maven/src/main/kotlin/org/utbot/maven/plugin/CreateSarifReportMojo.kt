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
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.gradle.plugin.CreateSarifReportFacade
import org.utbot.gradle.plugin.wrappers.TargetClassWrapper
import org.utbot.maven.plugin.wrappers.MavenProjectWrapper
import org.utbot.maven.plugin.wrappers.SourceFindingStrategyMaven
import java.io.File

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
    internal lateinit var generatedTestsRelativeRoot: String

    @Parameter(defaultValue = "target/generated/sarif")
    internal lateinit var sarifReportsRelativeRoot: String

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
            CreateSarifReportFacade.mergeReports(
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
            val sourceFindingStrategy =
                SourceFindingStrategyMaven(mavenProjectWrapper, targetClass.testsCodeFile.path)
            val createSarifReportFacade =
                CreateSarifReportFacade(sarifProperties, sourceFindingStrategy)
            createSarifReportFacade.generateForClass(
                targetClass, mavenProjectWrapper.workingDirectory, mavenProjectWrapper.runtimeClasspath
            )
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
