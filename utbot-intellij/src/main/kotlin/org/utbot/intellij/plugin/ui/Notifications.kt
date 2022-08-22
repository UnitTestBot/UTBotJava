package org.utbot.intellij.plugin.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.GotItMessage
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBFont
import java.awt.Point
import javax.swing.event.HyperlinkEvent

abstract class Notifier {
    protected abstract val notificationType: NotificationType
    protected abstract val displayId: String
    protected abstract fun content(project: Project?, module: Module?, info: String): String

    open fun notify(info: String, project: Project? = null, module: Module? = null) {
        notificationGroup
                .createNotification(content(project, module, info), notificationType)
                .notify(project)
    }

    protected open val notificationDisplayType = NotificationDisplayType.BALLOON

    protected val notificationGroup: NotificationGroup
        get() = NotificationGroup(displayId, notificationDisplayType)
}

abstract class WarningNotifier : Notifier() {
    override val notificationType: NotificationType = NotificationType.WARNING
    final override fun notify(info: String, project: Project?, module: Module?) {
        super.notify(info, project, module)
    }
}

abstract class ErrorNotifier : Notifier() {
    final override val notificationType: NotificationType = NotificationType.ERROR

    final override fun notify(info: String, project: Project?, module: Module?) {
        super.notify(info, project, module)
        error(content(project, module, info))
    }
}

object CommonErrorNotifier : ErrorNotifier() {
    override val displayId: String = "UTBot plugin errors"
    override fun content(project: Project?, module: Module?, info: String): String = info
}

object UnsupportedJdkNotifier : ErrorNotifier() {
    override val displayId: String = "Unsupported JDK"
    override fun content(project: Project?, module: Module?, info: String): String =
            "JDK versions older than 8 are not supported. This project's JDK version is $info"
}

object MissingLibrariesNotifier : WarningNotifier() {
    override val displayId: String = "Missing libraries"
    override fun content(project: Project?, module: Module?, info: String): String =
            "Library $info missing on the test classpath of module ${module?.name}"
}

@Suppress("unused")
object UnsupportedTestFrameworkNotifier : ErrorNotifier() {
    override val displayId: String = "Unsupported test framework"
    override fun content(project: Project?, module: Module?, info: String): String =
            "Test framework $info is not supported yet"
}

abstract class UrlNotifier : Notifier() {

    protected abstract val titleText: String
    protected abstract val urlOpeningListener: NotificationListener

    override fun notify(info: String, project: Project?, module: Module?) {
        notificationGroup
            .createNotification(
                titleText,
                content(project, module, info),
                notificationType,
                urlOpeningListener,
            ).notify(project)
    }
}

abstract class InformationUrlNotifier : UrlNotifier() {
    override val notificationType: NotificationType = NotificationType.INFORMATION
}

abstract class WarningUrlNotifier : UrlNotifier() {
    override val notificationType: NotificationType = NotificationType.WARNING
}

abstract class EventLogNotifier : InformationUrlNotifier() {
    override val notificationDisplayType = NotificationDisplayType.NONE
}

object SarifReportNotifier : EventLogNotifier() {

    override val displayId: String = "SARIF report"

    override val titleText: String = "" // no title

    override val urlOpeningListener: NotificationListener = NotificationListener.UrlOpeningListener(false)

    override fun content(project: Project?, module: Module?, info: String): String = info
}

object TestsReportNotifier : InformationUrlNotifier() {
    override val displayId: String = "Generated unit tests report"

    override val titleText: String = "UTBot: unit tests generated successfully"

    public override val urlOpeningListener: TestReportUrlOpeningListener = TestReportUrlOpeningListener

    override fun content(project: Project?, module: Module?, info: String): String = info
}

// TODO replace inheritance with decorators
object WarningTestsReportNotifier : WarningUrlNotifier() {
    override val displayId: String = "Generated unit tests report"

    override val titleText: String = "UTBot: unit tests generated with warnings"

    public override val urlOpeningListener: TestReportUrlOpeningListener = TestReportUrlOpeningListener

    override fun content(project: Project?, module: Module?, info: String): String = info
}

object DetailsTestsReportNotifier : EventLogNotifier() {
    override val displayId: String = "Test report details"

    override val titleText: String = "Test report details of the unit tests generation via UtBot"

    public override val urlOpeningListener: TestReportUrlOpeningListener = TestReportUrlOpeningListener

    override fun content(project: Project?, module: Module?, info: String): String = info
}

/**
 * Listener that handles URLs starting with [prefix], like "#utbot/configure-mockito".
 */
object TestReportUrlOpeningListener: NotificationListener.Adapter() {
    const val prefix = "#utbot/"
    const val mockitoSuffix = "configure-mockito"
    const val mockitoInlineSuffix = "mockito-inline"
    const val eventLogSuffix = "event-log"

    val callbacks: Map<String, MutableList<() -> Unit>> = hashMapOf(
        Pair(mockitoSuffix, mutableListOf()),
        Pair(mockitoInlineSuffix, mutableListOf()),
        Pair(eventLogSuffix, mutableListOf()),
    )

    private val defaultListener = NotificationListener.UrlOpeningListener(false)

    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        val description = e.description
        if (description.startsWith(prefix)) {
            handleDescription(description.removePrefix(prefix))
        } else {
            return defaultListener.hyperlinkUpdate(notification, e)
        }
    }

    private fun handleDescription(descriptionSuffix: String) =
        callbacks[descriptionSuffix]?.map { it() } ?: error("No such command with #utbot prefix: $descriptionSuffix")
}

object GotItTooltipActivity : StartupActivity {
    private const val KEY = "UTBot.GotItMessageWasShown"
    override fun runActivity(project: Project) {
        if (PropertiesComponent.getInstance().isTrueValue(KEY)) return
        ApplicationManager.getApplication().invokeLater {
            val shortcut = ActionManager.getInstance()
                .getKeyboardShortcut("org.utbot.intellij.plugin.ui.actions.GenerateTestsAction")?:return@invokeLater
            val shortcutText = KeymapUtil.getShortcutText(shortcut)
            val message = GotItMessage.createMessage("UTBot is ready!",
                "<div style=\"font-size:${JBFont.label().biggerOn(2.toFloat()).size}pt;\">" +
                        "You can get test coverage for methods, Java classes,<br>and even for whole source roots<br> with <b>$shortcutText</b></div>")
            message.setCallback { PropertiesComponent.getInstance().setValue(KEY, true) }
            WindowManager.getInstance().getFrame(project)?.rootPane?.let {
                message.show(RelativePoint(it, Point(it.width, it.height)), Balloon.Position.above)
            }
        }
    }
}
