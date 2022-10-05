package org.utbot.intellij.plugin.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import com.intellij.util.ReflectionUtil
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.util.application.invokeLater

/**
 * This object is required to encapsulate Android API usage and grant safe access to it.
 */
object IntelliJApiHelper {

    enum class Target { THREAD_POOL, READ_ACTION, WRITE_ACTION, EDT_LATER }

    fun run(target: Target, runnable: Runnable) {
        when (target) {
            Target.THREAD_POOL -> AppExecutorUtil.getAppExecutorService().submit {
                runnable.run()
            }

            Target.READ_ACTION -> runReadAction { runnable.run() }
            Target.WRITE_ACTION -> runWriteAction { runnable.run() }
            Target.EDT_LATER -> invokeLater { runnable.run() }
        }
    }

    private val isAndroidPluginAvailable: Boolean =
        !PluginManagerCore.isDisabled(PluginId.getId("org.jetbrains.android"))

    fun isAndroidStudio(): Boolean =
        isAndroidPluginAvailable && ("AndroidStudio" == PlatformUtils.getPlatformPrefix())

    fun androidGradleSDK(project: Project): String? {
        if (!isAndroidPluginAvailable) return null
        try {
            val finderClass = Class.forName("com.android.tools.idea.gradle.util.GradleProjectSettingsFinder")
            var method = ReflectionUtil.getMethod(finderClass, "findGradleProjectSettings", Project::class.java) ?: return null
            val gradleProjectSettings = method.invoke(project) ?: return null
            method = ReflectionUtil.getMethod(gradleProjectSettings.javaClass, "getGradleJvm") ?: return null
            val gradleJvm = method.invoke(gradleProjectSettings)
            return if (gradleJvm is String) gradleJvm else null
        } catch (e: Exception) {
            return null
        }
    }
}
