package org.utbot.intellij.plugin.python

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.python.psi.PyFunction

object PythonActionMethods {
    const val pythonID = "Python"

    fun actionPerformed(e: AnActionEvent) {
    }

    fun update(e: AnActionEvent) {
    }

    fun getPsiTarget(e: AnActionEvent): Pair<Set<PyFunction>, PyFunction?> {
    }
}