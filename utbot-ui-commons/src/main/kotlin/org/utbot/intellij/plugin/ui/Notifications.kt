package org.utbot.intellij.plugin.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
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
import com.intellij.util.ui.UIUtil
import java.awt.Point
import javax.swing.event.HyperlinkEvent
import mu.KotlinLogging

abstract class Notifier {
    protected val logger = KotlinLogging.logger {}

    protected abstract val notificationType: NotificationType
    protected abstract val displayId: String
    protected open fun content(project: Project?, module: Module?, info: String): String = info

    open fun notify(info: String, project: Project? = null, module: Module? = null) {
        notify(info, project, module, AnAction.EMPTY_ARRAY)
    }

    open fun notify(info: String, project: Project? = null, module: Module? = null, actions: Array<AnAction>) {
        notificationGroup
            .createNotification(content(project, module, info), notificationType)
            .apply { actions.forEach { this.addAction(it) } }
                .notify(project)
    }

    protected open val notificationDisplayType = NotificationDisplayType.BALLOON

    protected val notificationGroup: NotificationGroup
        get() = NotificationGroup.findRegisteredGroup(displayId) ?: NotificationGroup(displayId, notificationDisplayType)
}

abstract class WarningNotifier : Notifier() {
    override val notificationType: NotificationType = NotificationType.WARNING
    final override fun notify(info: String, project: Project?, module: Module?) {
        super.notify(info, project, module)
    }
}

abstract class ErrorNotifier : Notifier() {
    final override val notificationType: NotificationType = NotificationType.ERROR

    override fun notify(info: String, project: Project?, module: Module?) {
        super.notify(info, project, module)
        error(content(project, module, info))
    }
}

object CommonErrorNotifier : ErrorNotifier() {
    override val displayId: String = "UTBot plugin errors"
}

class CommonLoggingNotifier(val type :NotificationType = NotificationType.WARNING) : Notifier() {
    override val displayId: String = "UTBot plugin errors"
    override val notificationType = type

    override fun notify(info: String, project: Project?, module: Module?) {
        super.notify(info, project, module)
        when (notificationType) {
            NotificationType.WARNING -> logger.warn(content(project, module, info))
            NotificationType.INFORMATION -> logger.info(content(project, module, info))
            else -> logger.error(content(project, module, info))
        }
    }
}

object UnsupportedJdkNotifier : ErrorNotifier() {
    override val displayId: String = "Unsupported JDK"
    override fun content(project: Project?, module: Module?, info: String): String =
            "JDK versions older than 8 are not supported. This project's JDK version is $info"
}

object InvalidClassNotifier : WarningNotifier() {
    override val displayId: String = "Invalid class"
    override fun content(project: Project?, module: Module?, info: String): String =
        "Generate tests with UnitTestBot for the $info is not supported."
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
        notificationGroup.createNotification(content(project, module, info), notificationType)
            .setTitle(titleText).setListener(urlOpeningListener).notify(project)
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
}

object TestsReportNotifier : InformationUrlNotifier() {
    override val displayId: String = "Generated unit tests report"

    override val titleText: String = "UnitTestBot: unit tests generated successfully"

    public override val urlOpeningListener: TestReportUrlOpeningListener = TestReportUrlOpeningListener
}

// TODO replace inheritance with decorators
object WarningTestsReportNotifier : WarningUrlNotifier() {
    override val displayId: String = "Generated unit tests report"

    override val titleText: String = "UnitTestBot: unit tests generated with warnings"

    public override val urlOpeningListener: TestReportUrlOpeningListener = TestReportUrlOpeningListener
}

object DetailsTestsReportNotifier : EventLogNotifier() {
    override val displayId: String = "Test report details"

    override val titleText: String = "Test report details of the unit tests generation via UnitTestBot"

    public override val urlOpeningListener: TestReportUrlOpeningListener = TestReportUrlOpeningListener
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
            val message = GotItMessage.createMessage("UnitTestBot is ready!",
                "<div style=\"font-size:${JBFont.label().biggerOn(2.toFloat()).size}pt;color:#${UIUtil.colorToHex(UIUtil.getLabelForeground())};\">" +
                        "You can get test coverage for methods, Java classes,<br>and even for whole source roots<br> with <b>$shortcutText</b></div>")
            message.setCallback { PropertiesComponent.getInstance().setValue(KEY, true) }
            WindowManager.getInstance().getFrame(project)?.rootPane?.let {
                message.show(RelativePoint(it, Point(it.width, it.height)), Balloon.Position.above)
            }
        }
    }
}
