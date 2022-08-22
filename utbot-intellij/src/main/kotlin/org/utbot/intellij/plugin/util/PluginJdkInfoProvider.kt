package org.utbot.intellij.plugin.util

import org.utbot.common.PathUtil.toPath
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.plugin.services.fetchJavaVersion

class PluginJdkInfoProvider(
    private val project: Project
) : JdkInfoDefaultProvider() {

    private val sdk: Sdk?
        get() {
            if (IntelliJApiHelper.isAndroidStudio()) {
                // Get Gradle JDK for Android
                IntelliJApiHelper.androidGradleSDK(project)
                    ?.let { sdkName ->
                        ProjectJdkTable.getInstance().findJdk(sdkName) ?.let {
                            return it
                        }
                    }
            }

            // Use Project SDK as analyzed JDK
            return ProjectRootManager.getInstance(project).projectSdk
        }

    override val info: JdkInfo
        get() = JdkInfo(
            sdk?.homePath?.toPath() ?: super.info.path, // Return default JDK in case of failure
            fetchJavaVersion(sdk?.versionString!!) // Return default JDK in case of failure
        )
}