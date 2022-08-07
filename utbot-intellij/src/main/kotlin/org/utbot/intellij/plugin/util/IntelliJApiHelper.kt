package org.utbot.intellij.plugin.util

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.gradle.util.GradleProjectSettingsFinder
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext

/**
 * This object is required to encapsulate Android API usage and grant safe access to it.
 */
object IntelliJApiHelper {

    enum class Target { THREAD_POOL, READ_ACTION, WRITE_ACTION, EDT_LATER }

    fun run(target: Target, runnable: Runnable) {
        UtContext.currentContext()?.let {
            when (target) {
                Target.THREAD_POOL -> AppExecutorUtil.getAppExecutorService().submit {
                    withUtContext(it) {
                        runnable.run()
                    }
                }
                Target.READ_ACTION -> runReadAction { withUtContext(it) { runnable.run() } }
                Target.WRITE_ACTION -> runWriteAction { withUtContext(it) { runnable.run() } }
                Target.EDT_LATER -> invokeLater { withUtContext(it) { runnable.run() } }
            }
        } ?: error("No context in thread ${Thread.currentThread()}")
    }

    private val isAndroidPluginAvailable: Boolean = !PluginManagerCore.isDisabled(PluginId.getId("org.jetbrains.android"))

    fun isAndroidStudio(): Boolean =
        isAndroidPluginAvailable && IdeInfo.getInstance().isAndroidStudio

    fun androidGradleSDK(project: Project): String? {
        return if (isAndroidPluginAvailable) GradleProjectSettingsFinder.getInstance().findGradleProjectSettings(project)?.gradleJvm
                else null
    }
}
