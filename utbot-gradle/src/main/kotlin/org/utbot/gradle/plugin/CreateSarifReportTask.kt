package org.utbot.gradle.plugin

import mu.KLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.utbot.common.bracket
import org.utbot.common.debug
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import org.utbot.gradle.plugin.wrappers.GradleProjectWrapper
import org.utbot.gradle.plugin.wrappers.SourceFindingStrategyGradle
import org.utbot.gradle.plugin.wrappers.SourceSetWrapper
import org.utbot.gradle.plugin.wrappers.TargetClassWrapper
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
            CreateSarifReportFacade.mergeReports(
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
            val sourceFindingStrategy =
                SourceFindingStrategyGradle(sourceSet, targetClass.testsCodeFile.path)
            val createSarifReportFacade =
                CreateSarifReportFacade(sarifProperties, sourceFindingStrategy)
            createSarifReportFacade.generateForClass(
                targetClass, sourceSet.workingDirectory, sourceSet.runtimeClasspath
            )
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
