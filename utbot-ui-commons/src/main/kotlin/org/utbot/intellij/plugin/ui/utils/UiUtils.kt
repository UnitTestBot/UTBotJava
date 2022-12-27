package org.utbot.intellij.plugin.ui.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

fun underProgress(project: Project, title:String, block: (indicator: ProgressIndicator) -> Unit) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, title) {
        override fun run(indicator: ProgressIndicator) {
            return block(indicator)
        }
    })
}