package org.utbot.intellij.plugin.error

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

fun showErrorDialogLater(project: Project, message: String, title: String) {
    invokeLater {
        Messages.showErrorDialog(project, message, title)
    }
}