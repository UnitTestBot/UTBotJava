package org.utbot.intellij.plugin.ui.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

open class BaseTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
) {
    var testSourceRoot: VirtualFile? = null
}
