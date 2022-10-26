package org.utbot.intellij.plugin.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.util.lang.JavaVersion

fun findSdkVersion(module:Module): JavaVersion =
    findSdkVersionOrNull(module) ?: error("Cannot define sdk version in module $module")

fun findSdkVersionOrNull(module: Module): JavaVersion? {
    val moduleSdk = ModuleRootManager.getInstance(module).sdk
    return JavaVersion.tryParse(moduleSdk?.versionString)
}


