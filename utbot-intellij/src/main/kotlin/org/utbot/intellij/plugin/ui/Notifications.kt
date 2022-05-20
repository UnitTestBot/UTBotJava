package org.utbot.intellij.plugin.ui

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

abstract class Notifier {
    protected abstract val notificationType: NotificationType
    protected abstract val displayId: String
    protected abstract fun content(project: Project?, module: Module?, info: String): String

    open fun notify(info: String, project: Project? = null, module: Module? = null) {
        notificationGroup
                .createNotification(content(project, module, info), notificationType)
                .notify(project)
    }

    protected val notificationGroup: NotificationGroup
        get() = NotificationGroup(displayId, NotificationDisplayType.BALLOON)
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

    override fun notify(info: String, project: Project?, module: Module?) {
        notificationGroup
            .createNotification(
                titleText,
                content(project, module, info),
                notificationType,
                NotificationListener.UrlOpeningListener(false)
            ).notify(project)
    }
}

abstract class InformationUrlNotifier : UrlNotifier() {
    override val notificationType: NotificationType = NotificationType.INFORMATION
}

object SarifReportNotifier : InformationUrlNotifier() {

    override val displayId: String = "SARIF report"

    override val titleText: String = "" // no title

    override fun content(project: Project?, module: Module?, info: String): String = info
}

object TestsReportNotifier : InformationUrlNotifier() {
    override val displayId: String = "Generated unit tests report"

    override val titleText: String = "Report of the unit tests generation via UtBot"

    override fun content(project: Project?, module: Module?, info: String): String = info
}
