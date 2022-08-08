package org.utbot.intellij.plugin.util

import org.utbot.common.PathUtil.toPath
import org.utbot.framework.JdkPathDefaultProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import java.nio.file.Path

class PluginJdkPathProvider(
    private val project: Project,
    private val testModule: Module,
) : JdkPathDefaultProvider() {

    override val jdkPath: Path
        get() =
            if (IntelliJApiHelper.isAndroidStudio()) {
                // Get Gradle JDK for Android
                IntelliJApiHelper.androidGradleSDK(project)
                    ?.let { sdkName ->
                        ProjectJdkTable.getInstance().findJdk(sdkName)?.homePath?.toPath()
                    }
            } else {
                // Use testModule JDK (or Project SDK) as analyzed JDK
                (ModuleRootManager.getInstance(testModule).sdk
                    ?.homePath ?: ProjectRootManager.getInstance(project).projectSdk?.homePath)
                    ?.toPath()
            } ?: super.jdkPath // Return default JDK in case of failure

}