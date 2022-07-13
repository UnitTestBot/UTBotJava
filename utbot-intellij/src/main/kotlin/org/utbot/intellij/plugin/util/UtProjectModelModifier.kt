package org.utbot.intellij.plugin.util

import com.intellij.codeInsight.daemon.impl.quickfix.LocateLibraryDialog
import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.ide.JavaUiBundle
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
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.ui.Messages
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import java.util.stream.Collectors
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor

class UtProjectModelModifier(val project: Project) : IdeaProjectModelModifier(project) {
    override fun addExternalLibraryDependency(
        modules: Collection<Module>,
        descriptor: ExternalLibraryDescriptor,
        scope: DependencyScope
    ): Promise<Void> {
        val defaultRoots = descriptor.libraryClassesRoots
        val firstModule = ContainerUtil.getFirstItem(modules) ?: return rejectedPromise()
        val classesRoots = if (defaultRoots.isNotEmpty()) {
            LocateLibraryDialog(
                firstModule,
                defaultRoots,
                descriptor.presentableName
            ).showAndGetResult()
        } else {
            val libraryProperties = RepositoryLibraryProperties(JpsMavenRepositoryLibraryDescriptor(descriptor.mavenCoordinates()))
            val roots = JarRepositoryManager.loadDependenciesModal(
                project,
                libraryProperties,
                /* loadSources = */ false,
                /* loadJavadoc = */ false,
                /* copyTo = */ null,
                /* repositories = */ null
            )
            if (roots.isEmpty()) {
                @Suppress("SpellCheckingInspection")
                Messages.showErrorDialog(
                    project,
                    JavaUiBundle.message("dialog.mesage.0.was.not.loaded", descriptor.presentableName),
                    JavaUiBundle.message("dialog.title.failed.to.download.library")
                )
                return rejectedPromise()
            }
            roots.stream()
                .filter { root: OrderRoot -> root.type === OrderRootType.CLASSES }
                .map { root: OrderRoot ->
                    PathUtil.getLocalPath(
                        root.file
                    )
                }
                .collect(Collectors.toList())
        }
        if (classesRoots.isNotEmpty()) {
            val libraryName = if (classesRoots.size > 1) descriptor.presentableName else null
            val urls = OrderEntryFix.refreshAndConvertToUrls(classesRoots)
            if (modules.size == 1) {
                ModuleRootModificationUtil.addModuleLibrary(firstModule, libraryName, urls, emptyList(), scope)
            } else {
                WriteAction.run<RuntimeException> {
                    val library =
                        LibraryUtil.createLibrary(
                            LibraryTablesRegistrar.getInstance().getLibraryTable(project), descriptor.presentableName
                        )
                    val model = library.modifiableModel
                    for (url in urls) {
                        model.addRoot(url!!, OrderRootType.CLASSES)
                    }
                    model.commit()
                    for (module in modules) {
                        ModuleRootModificationUtil.addDependency(module, library, scope, false)
                    }
                }
            }
        }
        return resolvedPromise()
    }

    private fun ExternalLibraryDescriptor.mavenCoordinates() : String {
        return "$libraryGroupId:$libraryArtifactId:${preferredVersion ?: RepositoryLibraryDescription.ReleaseVersionId}"
    }
}