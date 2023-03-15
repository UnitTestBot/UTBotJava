package org.androidstudio.plugin.util

import com.android.tools.idea.model.AndroidModel
import com.intellij.facet.ProjectFacetManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.impl.IdeaProjectModelModifier
import com.intellij.openapi.roots.libraries.Library
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.concurrency.Promise

/*
NOTE: this is a wrapper for [UtAndroidGradleJavaProjectModelModifier].
The purpose of this wrapper is to avoid inheritance of [AndroidGradleJavaProjectModelModifier]
because it leads to crashes when Android plugin is disabled.
 */
class UtAndroidGradleJavaProjectModelModifierWrapper(val project: Project): IdeaProjectModelModifier(project)  {

    override fun addExternalLibraryDependency(
        modules: Collection<Module?>,
        descriptor: ExternalLibraryDescriptor,
        scope: DependencyScope
    ): Promise<Void?>? {
        if (!isAndroidGradleProject(project)) {
            return null
        }

        // NOTE: we use such DependencyScope to obtain `implementation`, not `testImplementation`
        // to deal with androidTest modules (there is no way to add `androidTestImplementation` additionally.
        return UtAndroidGradleJavaProjectModelModifier().addExternalLibraryDependency(modules, descriptor, DependencyScope.COMPILE)
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

        return ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID).stream()
            .anyMatch { AndroidModel.isRequired(it) }
    }
}