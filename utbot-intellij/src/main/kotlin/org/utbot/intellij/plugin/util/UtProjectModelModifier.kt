package org.utbot.intellij.plugin.util

import com.intellij.codeInsight.daemon.impl.quickfix.LocateLibraryDialog
import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.IdeaProjectModelModifier
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor

class UtProjectModelModifier(val project: Project) : IdeaProjectModelModifier(project) {
    override fun addExternalLibraryDependency(
        modules: Collection<Module>,
        descriptor: ExternalLibraryDescriptor,
        scope: DependencyScope
    ): Promise<Void>? {
        val defaultRoots = descriptor.libraryClassesRoots
        val firstModule = ContainerUtil.getFirstItem(modules) ?: return null
        val classesRoots = if (defaultRoots.isNotEmpty()) {
            LocateLibraryDialog(
                firstModule,
                defaultRoots,
                descriptor.presentableName
            ).showAndGetResult()
        } else {
            val roots = JarRepositoryManager.loadDependenciesModal(
                project,
                RepositoryLibraryProperties(JpsMavenRepositoryLibraryDescriptor(descriptor.mavenCoordinates())),
                /* loadSources = */ false,
                /* loadJavadoc = */ false,
                /* copyTo = */ null,
                /* repositories = */ null
            )
            if (roots.isEmpty()) {
                return null
            }
            roots.filter { orderRoot -> orderRoot.type === OrderRootType.CLASSES }
                .map { PathUtil.getLocalPath(it.file) }.toList()
        }
        if (classesRoots.isNotEmpty()) {
            val urls = OrderEntryFix.refreshAndConvertToUrls(classesRoots)
            if (modules.size == 1) {
                ModuleRootModificationUtil.addModuleLibrary(
                    firstModule,
                    if (classesRoots.size > 1) descriptor.presentableName else null,
                    urls,
                    emptyList(),
                    scope
                )
            } else {
                WriteAction.run<RuntimeException> {
                    LibraryUtil.createLibrary(
                        LibraryTablesRegistrar.getInstance().getLibraryTable(project),
                        descriptor.presentableName
                    ).let {
                        val model = it.modifiableModel
                        urls.forEach { url -> model.addRoot(url, OrderRootType.CLASSES) }
                        model.commit()
                        modules.forEach { module ->
                            ModuleRootModificationUtil.addDependency(module, it, scope, false)
                        }
                    }
                }
            }
        }
        return resolvedPromise()
    }

    private fun ExternalLibraryDescriptor.mavenCoordinates(): String {
        return "$libraryGroupId:$libraryArtifactId:${preferredVersion ?: RepositoryLibraryDescription.ReleaseVersionId}"
    }
}