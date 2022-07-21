package org.utbot.intellij.plugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class Configurable(val project: Project) : SearchableConfigurable {
    private val displayName: String = "UtBot Configuration"
    private val id: String = "org.utbot.intellij.plugin.settings.UtBotSettingsConfigurable"
    private val settingsWindow = SettingsWindow(project)

    override fun createComponent(): JComponent = settingsWindow.panel

    override fun isModified(): Boolean = settingsWindow.isModified()

    override fun apply() = settingsWindow.apply()

    override fun reset() = settingsWindow.reset()

    override fun getDisplayName(): String = displayName

    override fun getId(): String = id
}