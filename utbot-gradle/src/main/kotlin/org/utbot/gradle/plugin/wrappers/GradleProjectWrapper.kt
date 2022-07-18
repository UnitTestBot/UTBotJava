package org.utbot.gradle.plugin.wrappers

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.utbot.common.FileUtil.createNewFileWithParentDirectories
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import java.io.File
import java.nio.file.Paths

/**
 * Contains information about the gradle project for which we are creating a SARIF report.
 */
class GradleProjectWrapper(
    val project: Project,
    val sarifProperties: SarifGradleExtensionProvider
) {

    private val javaPlugin: JavaPluginConvention =
        project.convention.getPlugin(JavaPluginConvention::class.java)

    /**
     * Contains child projects (~ gradle modules) of the [project].
     */
    val childProjects: List<GradleProjectWrapper> by lazy {
        project.childProjects.values.map { childProject ->
            GradleProjectWrapper(childProject, sarifProperties)
        }
    }

    /**
     * Contains source sets of the [project], except 'test' source set.
     */
    val sourceSets: List<SourceSetWrapper> by lazy {
        javaPlugin.sourceSets.filter { sourceSet ->
            sourceSet.name != SourceSet.TEST_SOURCE_SET_NAME
        }.map { sourceSet ->
            SourceSetWrapper(sourceSet, parentProject = this)
        }
    }

    /**
     * Directory for generated tests. For example, "build/generated/test/".
     */
    val generatedTestsDirectory: File by lazy {
        project.projectDir.resolve(sarifProperties.generatedTestsRelativeRoot).apply { mkdirs() }
    }

    /**
     * Directory for created SARIF reports. For example, "build/generated/sarif/".
     */
    val generatedSarifDirectory: File by lazy {
        project.projectDir.resolve(sarifProperties.sarifReportsRelativeRoot).apply { mkdirs() }
    }

    /**
     * SARIF report file containing results from all other reports from the [project].
     */
    val sarifReportFile: File by lazy {
        Paths.get(
            generatedSarifDirectory.path,
            sarifProperties.mergedSarifReportFileName ?: "${project.name}Report.sarif"
        ).toFile().apply {
            createNewFileWithParentDirectories()
        }
    }
}