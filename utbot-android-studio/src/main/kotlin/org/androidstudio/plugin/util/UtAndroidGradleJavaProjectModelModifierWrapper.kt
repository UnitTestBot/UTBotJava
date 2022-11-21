package org.androidstudio.plugin.util

import com.android.tools.idea.project.AndroidProjectInfo
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.impl.IdeaProjectModelModifier
import com.intellij.openapi.roots.libraries.Library
import com.intellij.pom.java.LanguageLevel
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.concurrency.Promise

class UtAndroidGradleJavaProjectModelModifierWrapper(val project: Project): IdeaProjectModelModifier(project)  {

    override fun addExternalLibraryDependency(
        modules: Collection<Module?>,
        descriptor: ExternalLibraryDescriptor,
        scope: DependencyScope
    ): Promise<Void?>? {

        val module = ContainerUtil.getFirstItem(modules) ?: return null
        if (!isAndroidGradleProject(module.project)) {
            return null
        }

        return UtAndroidGradleJavaProjectModelModifier().addExternalLibraryDependency(modules, descriptor, scope)
    }

    override fun addModuleDependency(
        from: Module,
        to: Module,
        scope: DependencyScope,
        exported: Boolean
    ): Promise<Void>? = null

    override fun addLibraryDependency(
        from: Module,
        library: Library,
        scope: DependencyScope,
        exported: Boolean
    ): Promise<Void>? = null

    override fun changeLanguageLevel(module: Module, level: LanguageLevel): Promise<Void>? = null

    private fun isAndroidGradleProject(project: Project): Boolean {
        val pluginId = PluginId.findId("org.jetbrains.android")
        if (pluginId == null || PluginManager.getInstance().findEnabledPlugin(pluginId) == null) {
            return false
        }

        return AndroidProjectInfo.getInstance(project).requiresAndroidModel()
    }
}