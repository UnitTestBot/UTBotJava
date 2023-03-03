package org.utbot.gradle.plugin

import mu.KLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.utbot.common.measureTime
import org.utbot.common.debug
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.sarif.GenerateTestsAndSarifReportFacade
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import org.utbot.gradle.plugin.wrappers.GradleProjectWrapper
import org.utbot.gradle.plugin.wrappers.SourceFindingStrategyGradle
import org.utbot.gradle.plugin.wrappers.SourceSetWrapper
import org.utbot.framework.plugin.sarif.TargetClassWrapper
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import java.io.File
import java.net.URLClassLoader
import javax.inject.Inject

/**
 * The main class containing the entry point [generateTestsAndSarifReport].
 *
 * [Documentation](https://docs.gradle.org/current/userguide/custom_tasks.html)
 */
open class GenerateTestsAndSarifReportTask @Inject constructor(
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
    fun generateTestsAndSarifReport() {
        // the user specifies the parameters using "-Pname=value"
        sarifProperties.taskParameters = project.gradle.startParameter.projectProperties
        val rootGradleProject = try {
            GradleProjectWrapper(project, sarifProperties)
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while configuring the gradle task" }
            return
        }
        try {

            generateForProjectRecursively(rootGradleProject)
            GenerateTestsAndSarifReportFacade.mergeReports(
                sarifReports = rootGradleProject.collectReportsRecursively(),
                mergedSarifReportFile = rootGradleProject.sarifReportFile
            )
        } catch (t: Throwable) {
            logger.error(t) { "Unexpected error while generating SARIF report" }
            return
        }
    }

    // internal

    // overwriting the getLogger() function from the DefaultTask
    private val logger: KLogger = org.utbot.gradle.plugin.logger

    private val dependencyPaths by lazy {
        val thisClassLoader = this::class.java.classLoader as? URLClassLoader
            ?: return@lazy System.getProperty("java.class.path")
        thisClassLoader.urLs.joinToString(File.pathSeparator) { it.path }
    }

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
        logger.debug().measureTime({ "Generating tests for the '${sourceSet.sourceSet.name}' source set" }) {
            withUtContext(UtContext(sourceSet.classLoader)) {
                val testCaseGenerator =
                    TestCaseGenerator(
                        listOf(sourceSet.workingDirectory),
                        sourceSet.runtimeClasspath,
                        dependencyPaths,
                        JdkInfoDefaultProvider().info
                    )
                sourceSet.targetClasses.forEach { targetClass ->
                    generateForClass(sourceSet, targetClass, testCaseGenerator)
                }
            }
        }
    }

    /**
     * Generates tests and a SARIF report for the class [targetClass].
     */
    private fun generateForClass(
        sourceSet: SourceSetWrapper,
        targetClass: TargetClassWrapper,
        testCaseGenerator: TestCaseGenerator,
    ) {
        logger.debug().measureTime({ "Generating tests for the $targetClass" }) {
            val sourceFindingStrategy = SourceFindingStrategyGradle(sourceSet, targetClass.testsCodeFile.path)
            GenerateTestsAndSarifReportFacade(sarifProperties, sourceFindingStrategy, testCaseGenerator)
                .generateForClass(targetClass, sourceSet.workingDirectory)
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
}
