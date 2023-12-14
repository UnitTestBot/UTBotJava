package org.utbot.intellij.plugin.python.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant

class GenerateTestsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        LanguageAssistant.get(e)?.actionPerformed(e)
    }

    override fun update(e: AnActionEvent) {
        val languageAssistant = LanguageAssistant.get(e)
        if (languageAssistant == null) {
            e.presentation.isEnabled = false
        } else {
            languageAssistant.update(e)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
