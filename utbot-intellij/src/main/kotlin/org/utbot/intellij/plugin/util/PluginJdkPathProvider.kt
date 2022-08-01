package org.utbot.intellij.plugin.util

import org.utbot.common.PathUtil.toPath
import org.utbot.framework.JdkPathDefaultProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import java.nio.file.Path

class PluginJdkPathProvider(
    private val project: Project
) : JdkPathDefaultProvider() {

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

    override val jdkPath: Path
        get() = sdk?.let { it.homePath?.toPath() } ?: super.jdkPath // Return default JDK in case of failure

    override val jdkVersion: String
        get() = sdk?.versionString ?: super.jdkVersion // Return default JDK in case of failure
}