package org.utbot.intellij.plugin.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModifier
import com.intellij.openapi.util.Trinity
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.xml.XmlFile
import com.intellij.util.ThrowableRunnable
import com.intellij.util.xml.DomUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.idea.maven.dom.MavenDomBundle
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.converters.MavenDependencyCompletionUtil
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager

class UtMavenProjectModelModifier(val project: Project): JavaProjectModelModifier() {

    private val mavenProjectsManager = MavenProjectsManager.getInstance(project)

    override fun addExternalLibraryDependency(
        modules: Collection<Module>,
        descriptor: ExternalLibraryDescriptor,
        scope: DependencyScope
    ): Promise<Void?>? {
        for (module in modules) {
            if (!mavenProjectsManager.isMavenizedModule(module)) {
                return null
            }
        }

        val mavenId = MavenId(descriptor.libraryGroupId, descriptor.libraryArtifactId, descriptor.preferredVersion)
        return addDependency(modules, mavenId, descriptor.preferredVersion, scope)
    }

    override fun changeLanguageLevel(module: Module, level: LanguageLevel): Promise<Void> = rejectedPromise()

    private fun addDependency(
        fromModules: Collection<Module>,
        mavenId: MavenId,
        preferredVersion: String?,
        scope: DependencyScope,
    ): Promise<Void?>? {
        val models: MutableList<Trinity<MavenDomProjectModel, MavenId, String?>> = ArrayList(fromModules.size)
        val files: MutableList<XmlFile> = ArrayList(fromModules.size)
        val projectToUpdate: MutableList<MavenProject> = ArrayList(fromModules.size)
        val mavenScope = getMavenScope(scope)

        for (from in fromModules) {
            if (!mavenProjectsManager.isMavenizedModule(from)) return null
            val fromProject: MavenProject = mavenProjectsManager.findProject(from) ?: return null
            val model = MavenDomUtil.getMavenDomProjectModel(project, fromProject.file) ?: return null
            var scopeToSet: String? = null
            var version: String? = null
            if (mavenId.groupId != null && mavenId.artifactId != null) {
                val managedDependency = MavenDependencyCompletionUtil.findManagedDependency(
                    model, project,
                    mavenId.groupId!!,
                    mavenId.artifactId!!
                )
                if (managedDependency != null) {
                    val managedScope = StringUtil.nullize(managedDependency.scope.stringValue, true)
                    scopeToSet = if (managedScope == null && MavenConstants.SCOPE_COMPILE == mavenScope ||
                        StringUtil.equals(managedScope, mavenScope)
                    ) null else mavenScope
                }
                if (managedDependency == null || StringUtil.isEmpty(managedDependency.version.stringValue)) {
                    version = preferredVersion
                    scopeToSet = mavenScope
                }
            }
            models.add(Trinity.create(model, MavenId(mavenId.groupId, mavenId.artifactId, version), scopeToSet))
            files.add(DomUtil.getFile(model))
            projectToUpdate.add(fromProject)
        }

        WriteCommandAction.writeCommandAction(project, *PsiUtilCore.toPsiFileArray(files))
            .withName(MavenDomBundle.message("fix.add.dependency")).run(
                ThrowableRunnable<RuntimeException> {
                    val pdm = PsiDocumentManager.getInstance(project)
                    for (trinity in models) {
                        val model = trinity.first
                        val dependency = MavenDomUtil.createDomDependency(model, null, trinity.second)
                        val ms = trinity.third
                        if (ms != null) {
                            dependency.scope.stringValue = ms
                        }
                        val document =
                            pdm.getDocument(DomUtil.getFile(model))
                        if (document != null) {
                            pdm.doPostponedOperationsAndUnblockDocument(document)
                            FileDocumentManager.getInstance().saveDocument(document)
                        }
                    }
                })

        return mavenProjectsManager.forceUpdateProjects(projectToUpdate)
    }

    private fun getMavenScope(scope: DependencyScope): String? = when (scope) {
        DependencyScope.RUNTIME -> MavenConstants.SCOPE_RUNTIME
        DependencyScope.COMPILE -> MavenConstants.SCOPE_COMPILE
        DependencyScope.TEST -> MavenConstants.SCOPE_TEST
        DependencyScope.PROVIDED -> MavenConstants.SCOPE_PROVIDED
        else -> throw IllegalArgumentException(scope.toString())
    }
}