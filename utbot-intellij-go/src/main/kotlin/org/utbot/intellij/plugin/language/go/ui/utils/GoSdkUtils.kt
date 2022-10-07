package org.utbot.intellij.plugin.language.go.ui.utils

import com.goide.sdk.GoSdk
import java.nio.file.Paths

fun GoSdk.resolveGoExecutablePath(): String? {
    val canonicalGoSdkPath = this.executable?.canonicalPath ?: return null
    return Paths.get(canonicalGoSdkPath).toAbsolutePath().toString()
}