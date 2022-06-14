package org.utbot.gradle.plugin

import mu.KotlinLogging
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.utbot.gradle.plugin.extension.SarifGradleExtension
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import java.io.File

internal val logger = KotlinLogging.logger {}

/**
 * The main class containing the entry point [apply].
 *
 * [Documentation](https://docs.gradle.org/current/userguide/custom_plugins.html)
 */
@Suppress("unused")
class SarifGradlePlugin : Plugin<Project> {

    /**
     * The name of the gradle extension.
     * @see [SarifGradleExtension]
     */
    internal val sarifReportExtensionName = "sarifReport"

    /**
     * The name of the gradle task.
     * @see [GenerateTestsAndSarifReportTask]
     */
    internal val generateTestsAndSarifReportTaskName = "generateTestsAndSarifReport"

    /**
     * Entry point: called when the plugin is applied.
     */
    override fun apply(project: Project) {
        val sarifGradleExtension = project.extensions.create(
            sarifReportExtensionName,
            SarifGradleExtension::class.java
        )
        val sarifGradleExtensionProvider = SarifGradleExtensionProvider(
            project,
            sarifGradleExtension
        )

        val generateTestsAndSarifReportTask = project.tasks.register(
            generateTestsAndSarifReportTaskName,
            GenerateTestsAndSarifReportTask::class.java,
            sarifGradleExtensionProvider
        )
        generateTestsAndSarifReportTask.addDependencyOnClassesTasksRecursively(project)

        markGeneratedTestsDirectoryIfNeededRecursively(project, sarifGradleExtensionProvider)
    }

    // internal

    /**
     * Applies [addDependencyOnClassesTasks] to the [project] and to all its child projects.
     */
    private fun TaskProvider<GenerateTestsAndSarifReportTask>.addDependencyOnClassesTasksRecursively(project: Project) {
        project.afterEvaluate {
            addDependencyOnClassesTasks(project)
        }
        project.childProjects.values.forEach { childProject ->
            addDependencyOnClassesTasksRecursively(childProject)
        }
    }

    /**
     * Makes [GenerateTestsAndSarifReportTask] dependent on `classes` task
     * of each source set from the [project], except `test` source set.
     *
     * The [project] should be evaluated because we need its `java` plugin.
     * Therefore, it is recommended to call this function in the `project.afterEvaluate` block.
     */
    private fun TaskProvider<GenerateTestsAndSarifReportTask>.addDependencyOnClassesTasks(project: Project) {
        val javaPlugin = project.convention.findPlugin(JavaPluginConvention::class.java)
        if (javaPlugin == null) {
            logger.warn {
                "JavaPlugin was not found for project '${project.name}' while adding dependencies on classes tasks"
            }
            return
        }
        val sourceSetsExceptTest = javaPlugin.sourceSets.filter { sourceSet ->
            sourceSet.name != SourceSet.TEST_SOURCE_SET_NAME
        }
        logger.debug { "Found source sets in the '${project.name}': ${sourceSetsExceptTest.map { it.name }}" }
        configure { generateTestsAndSarifReportTask ->
            sourceSetsExceptTest.map { sourceSet ->
                val classesTask = project.tasks.getByName(sourceSet.classesTaskName)
                generateTestsAndSarifReportTask.dependsOn(classesTask)
                logger.debug { "'generateTestsAndSarifReport' task now depends on the task '${classesTask.name}'" }
            }
        }
    }

    /**
     * Applies [markGeneratedTestsDirectoryIfNeeded] to the [project] and to all its child projects.
     */
    private fun markGeneratedTestsDirectoryIfNeededRecursively(
        project: Project,
        sarifGradleExtensionProvider: SarifGradleExtensionProvider
    ) {
        project.afterEvaluate {
            markGeneratedTestsDirectoryIfNeeded(project, sarifGradleExtensionProvider)
        }
        project.childProjects.values.forEach { childProject ->
            markGeneratedTestsDirectoryIfNeededRecursively(childProject, sarifGradleExtensionProvider)
        }
    }

    /**
     * Marks the directory `generatedTestsRelativeRoot` as `test sources root`.
     * The directory is specified relative to the [project] root.
     * Does nothing if markGeneratedTestsDirectoryAsTestSourcesRoot is false.
     *
     * The [project] should be evaluated because we need its `java` plugin.
     * Therefore, it is recommended to call this function in the `project.afterEvaluate` block.
     */
    private fun markGeneratedTestsDirectoryIfNeeded(
        project: Project,
        sarifGradleExtensionProvider: SarifGradleExtensionProvider
    ) {
        if (!sarifGradleExtensionProvider.markGeneratedTestsDirectoryAsTestSourcesRoot)
            return

        val javaPlugin = project.convention.findPlugin(JavaPluginConvention::class.java)
        if (javaPlugin == null) {
            logger.warn {
                "JavaPlugin was not found for project ${project.name} while marking the generated test folder"
            }
            return
        }

        val generatedTestsDirectory = File(sarifGradleExtensionProvider.generatedTestsRelativeRoot)
        javaPlugin.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME) {
            it.java.setSrcDirs(it.java.srcDirs + generatedTestsDirectory)
            logger.debug {
                "The file '${generatedTestsDirectory.absolutePath}' has been added to srcDirs of the 'test' source set"
            }
        }
    }
}
