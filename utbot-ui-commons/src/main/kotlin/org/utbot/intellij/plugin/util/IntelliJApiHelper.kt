package org.utbot.intellij.plugin.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import com.intellij.util.ReflectionUtil
import com.intellij.util.concurrency.AppExecutorUtil
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.util.application.invokeLater

/**
 * This object is required to encapsulate Android API usage and grant safe access to it.
 */
object IntelliJApiHelper {
    private val logger = KotlinLogging.logger {"org.utbot.IntelliJApiHelper"}
    enum class Target { THREAD_POOL, READ_ACTION, WRITE_ACTION, EDT_LATER }

    fun run(target: Target, indicator: ProgressIndicator? = null, logMessage : String, runnable: Runnable) {
        logger.info { "[${target}]: " + logMessage +
                if (indicator != null) ", indicator[${indicator.text}; ${(indicator.fraction * 100).toInt()}%]" else "" }
        if (indicator != null && indicator.isCanceled) {
            logger.error { "Indicator is already cancelled" }
            return
        }
        val wrapper = Runnable {
            try {
                runnable.run()
            } catch (e: Exception) {
                logger.error(e) { target.toString() }
                throw e
            }
        }
        when (target) {
            Target.THREAD_POOL -> AppExecutorUtil.getAppExecutorService().submit { wrapper.run() }
            Target.READ_ACTION -> runReadAction { wrapper.run() }
            Target.WRITE_ACTION -> runWriteAction { wrapper.run() }
            Target.EDT_LATER -> ApplicationManager.getApplication().invokeLater( wrapper, ModalityState.NON_MODAL )
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
