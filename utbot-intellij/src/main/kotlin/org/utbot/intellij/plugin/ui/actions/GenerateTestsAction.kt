package org.utbot.intellij.plugin.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant
import org.utbot.intellij.plugin.settings.Settings

class GenerateTestsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        LanguageAssistant.get(e)?.actionPerformed(e)
    }

    override fun update(e: AnActionEvent) {
        val languageAssistant = LanguageAssistant.get(e)
        if (languageAssistant == null || !accessByProjectSettings(e)) {
            e.presentation.isEnabled = false
        } else {
            languageAssistant.update(e)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun accessByProjectSettings(e: AnActionEvent): Boolean {
        val experimentalLanguageSetting = e.project?.service<Settings>()?.experimentalLanguagesSupport
        val languagePackageName = LanguageAssistant.get(e)?.toString()
        return experimentalLanguageSetting == true || languagePackageName?.contains("JvmLanguageAssistant") == true
    }
}
