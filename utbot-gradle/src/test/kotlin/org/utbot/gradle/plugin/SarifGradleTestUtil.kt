package org.utbot.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.testfixtures.ProjectBuilder

internal fun buildProject(): Project {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("java")
    project.pluginManager.apply("org.utbot.gradle.plugin")
    return project
}

internal val Project.sarifGradlePlugin: SarifGradlePlugin
    get() = this.plugins.getPlugin(SarifGradlePlugin::class.java)

internal val Project.javaPlugin: JavaPluginConvention
    get() = this.convention.getPlugin(JavaPluginConvention::class.java)

internal val Project.mainSourceSet: SourceSet
    get() = this.javaPlugin.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
