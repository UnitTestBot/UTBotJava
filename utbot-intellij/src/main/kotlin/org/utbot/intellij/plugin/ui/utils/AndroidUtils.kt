package org.utbot.intellij.plugin.ui.utils

import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.android.sdk.AndroidSdkType
import org.utbot.intellij.plugin.ui.CommonErrorNotifier
import org.utbot.intellij.plugin.ui.UnsupportedJdkNotifier

val Project.isBuildWithGradle
    get() = GradleProjectInfo.getInstance(this).isBuildWithGradle

/**
 * Obtain JDK version and make sure that it is JDK8 or JDK11
 */
private fun jdkVersionBy(sdk: Sdk?): JavaSdkVersion {
    if (sdk == null) {
        CommonErrorNotifier.notify("Failed to obtain JDK version of the project")
    }
    requireNotNull(sdk)

    val jdkVersion = when (sdk.sdkType) {
        is JavaSdk -> {
            (sdk.sdkType as JavaSdk).getVersion(sdk)
        }
        is AndroidSdkType -> {
            ((sdk.sdkType as AndroidSdkType).dependencyType as JavaSdk).getVersion(sdk)
        }
        else -> null
    }
    if (jdkVersion == null) {
        CommonErrorNotifier.notify("Failed to obtain JDK version of the project")
    }
    requireNotNull(jdkVersion)
    if (!jdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
        UnsupportedJdkNotifier.notify(jdkVersion.description)
    }
    return jdkVersion
}