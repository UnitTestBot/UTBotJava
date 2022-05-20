package org.utbot.intellij.plugin.util

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.gradle.util.GradleProjectSettingsFinder
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project

/**
 * This object is required to encapsulate Android API usage and grant safe access to it.
 */
object AndroidApiHelper {
    private val isAndroidPluginAvailable: Boolean = !PluginManagerCore.isDisabled(PluginId.getId("org.jetbrains.android"))

    fun isAndroidStudio(): Boolean =
        isAndroidPluginAvailable && IdeInfo.getInstance().isAndroidStudio

    fun gradleSDK(project: Project): String? {
        return if (isAndroidPluginAvailable) GradleProjectSettingsFinder.getInstance().findGradleProjectSettings(project)?.gradleJvm
                else null
    }
}
