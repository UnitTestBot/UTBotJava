package org.utbot.gradle.plugin

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SarifGradlePluginTest {

    @Test
    fun `project should contain the sarif plugin`() {
        val project = buildProject()
        val sarifGradlePlugin = project.plugins.getPlugin(SarifGradlePlugin::class.java)
        assertNotNull(sarifGradlePlugin)
    }

    @Test
    fun `plugin should register createSarifReport task`() {
        val project = buildProject()
        val sarifGradlePlugin = project.sarifGradlePlugin
        val createSarifReportTask = project.tasks.getByName(sarifGradlePlugin.createSarifReportTaskName)
        assertNotNull(createSarifReportTask)
    }

    @Test
    fun `plugin should register sarifReport extension`() {
        val project = buildProject()
        val sarifGradlePlugin = project.sarifGradlePlugin
        val sarifReportExtension = project.extensions.getByName(sarifGradlePlugin.sarifReportExtensionName)
        assertNotNull(sarifReportExtension)
    }
}