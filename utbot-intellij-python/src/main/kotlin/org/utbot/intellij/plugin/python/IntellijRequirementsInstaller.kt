package org.utbot.intellij.plugin.python

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import org.utbot.intellij.plugin.ui.Notifier
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.utils.RequirementsUtils
import javax.swing.JComponent
import org.jetbrains.concurrency.runAsync


class IntellijRequirementsInstaller(
    val project: Project,
): RequirementsInstaller {
    override fun checkRequirements(pythonPath: String, requirements: List<String>): Boolean {
        return RequirementsUtils.requirementsAreInstalled(pythonPath, requirements)
    }

    override fun installRequirements(pythonPath: String, requirements: List<String>) {
        invokeLater {
            if (InstallRequirementsDialog(requirements).showAndGet()) {
                runAsync {
                    val installResult = RequirementsUtils.installRequirements(pythonPath, requirements)
                    invokeLater {
                        if (installResult.exitValue != 0) {
                            showErrorDialogLater(
                                project,
                                "Requirements installing failed.<br>" +
                                        "${installResult.stderr}<br><br>" +
                                        "Try to install with pip:<br>" +
                                        " ${requirements.joinToString("<br>")}",
                                "Requirements error"
                            )
                        } else {
                            invokeLater {
                                runReadAction {
                                    PythonNotifier.notify("Requirements installation is complete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


class InstallRequirementsDialog(private val requirements: List<String>) : DialogWrapper(true) {
    init {
        title = "Python Requirements Installation"
        init()
    }

    private lateinit var panel: DialogPanel

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Some requirements are not installed.") { }
            row("Requirements:") { }
            indent {
                    requirements.map { row {text(it)} }
            }
            row("Install them?") { }
        }
        return panel
    }
}

object PythonNotifier : Notifier() {
    override val notificationType: NotificationType = NotificationType.INFORMATION

    override val displayId: String = "Python notification"
}
