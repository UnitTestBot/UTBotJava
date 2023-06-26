package org.utbot.intellij.plugin.language.python

import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import org.utbot.intellij.plugin.ui.Notifier
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.util.IntelliJApiHelper
import org.utbot.python.RequirementsInstaller
import org.utbot.python.utils.RequirementsUtils
import javax.swing.JComponent


class IntellijRequirementsInstaller(
    val project: Project,
): RequirementsInstaller {
    override fun checkRequirements(pythonPath: String, requirements: List<String>): Boolean {
        return RequirementsUtils.requirementsAreInstalled(pythonPath, requirements)
    }

    override fun installRequirements(pythonPath: String, requirements: List<String>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Installing requirements") {
            override fun run(indicator: ProgressIndicator) {
                IntelliJApiHelper.run(
                    IntelliJApiHelper.Target.EDT_LATER,
                    indicator,
                    "Installing requirements"
                ) {
                    if (InstallRequirementsDialog(requirements).showAndGet()) {
                        PythonNotifier.notify("Start requirements installation")
                        val installResult = RequirementsUtils.installRequirements(pythonPath, requirements)
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
                            PythonNotifier.notify("Requirements installation is complete")
                        }
                    }
                }
            }
        })
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
    override fun notify(info: String, project: Project?, module: Module?) {
        val contentText = content(project, module, info)
        notificationGroup.createNotification(contentText, notificationType)
        logger.info(contentText)
    }
}
