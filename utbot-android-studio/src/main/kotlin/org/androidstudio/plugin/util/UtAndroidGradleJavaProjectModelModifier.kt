package org.androidstudio.plugin.util

import com.android.tools.idea.gradle.AndroidGradleJavaProjectModelModifier
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencySpec
import com.android.tools.idea.gradle.dsl.api.dependencies.CommonConfigurationNames
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.android.tools.idea.gradle.project.sync.GradleSyncListener
import com.android.tools.idea.gradle.project.sync.idea.GradleSyncExecutor
import com.android.tools.idea.gradle.util.GradleUtil
import com.android.tools.idea.project.AndroidProjectInfo
import com.android.tools.idea.projectsystem.TestArtifactSearchScopes
import com.google.wireless.android.sdk.stats.GradleSyncStats
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise

class UtAndroidGradleJavaProjectModelModifier : AndroidGradleJavaProjectModelModifier() {
    override fun addExternalLibraryDependency(
        modules: Collection<Module?>,
        descriptor: ExternalLibraryDescriptor,
        scope: DependencyScope
    ): Promise<Void?>? {
        val module = ContainerUtil.getFirstItem(modules) ?: return null
        val dependencySpec = ArtifactDependencySpec.create(descriptor.libraryArtifactId, descriptor.libraryGroupId, descriptor.preferredVersion)
        return addExternalLibraryDependency(module, dependencySpec, scope)
    }

    private fun addExternalLibraryDependency(
        module: Module,
        dependencySpec: ArtifactDependencySpec,
        scope: DependencyScope,
    ): Promise<Void?>? {
        val project = module.project
        val openedFile = FileEditorManagerEx.getInstanceEx(project).currentFile
        val buildModelsToUpdate: MutableList<GradleBuildModel> = ArrayList()

        val buildModel = GradleBuildModel.get(module) ?: return null
        val configurationName = getConfigurationName(module, scope, openedFile)
        val dependencies = buildModel.dependencies()
        dependencies.addArtifact(configurationName, dependencySpec)
        buildModelsToUpdate.add(buildModel)

        WriteCommandAction.writeCommandAction(project).withName("Add Gradle Library Dependency").run<RuntimeException> {
            buildModelsToUpdate.forEach { buildModel -> buildModel.applyChanges() }
            registerUndoAction(project)
        }

        return doAndroidGradleSync(project, GradleSyncStats.Trigger.TRIGGER_MODIFIER_ADD_LIBRARY_DEPENDENCY)
    }

    private fun getConfigurationName(module: Module, scope: DependencyScope, openedFile: VirtualFile?): String =
        GradleUtil.mapConfigurationName(
            getLegacyConfigurationName(module, scope, openedFile),
            GradleUtil.getAndroidGradleModelVersionInUse(module),
            false
        )

    private fun getLegacyConfigurationName(
        module: Module,
        scope: DependencyScope,
        openedFile: VirtualFile?
    ): String {
        if (!scope.isForProductionCompile) {
            val testScopes = TestArtifactSearchScopes.getInstance(module)
            if (testScopes != null && openedFile != null) {
                return if (testScopes.isAndroidTestSource(openedFile)) CommonConfigurationNames.ANDROID_TEST_COMPILE else CommonConfigurationNames.TEST_COMPILE
            }
        }
        return CommonConfigurationNames.COMPILE
    }

    private fun registerUndoAction(project: Project) {
        UndoManager.getInstance(project).undoableActionPerformed(object : BasicUndoableAction() {

            override fun undo() {
                doAndroidGradleSync(project, GradleSyncStats.Trigger.TRIGGER_MODIFIER_ACTION_UNDONE)

            }

            override fun redo() {
                doAndroidGradleSync(project, GradleSyncStats.Trigger.TRIGGER_MODIFIER_ACTION_REDONE)
            }
        })
    }

    private fun doAndroidGradleSync(project: Project, trigger: GradleSyncStats.Trigger): AsyncPromise<Void?> {
        val promise = AsyncPromise<Void?>()
        val request = GradleSyncInvoker.Request(trigger)
        val listener = object : GradleSyncListener {
            override fun syncSucceeded(project: Project) {
                promise.setResult(null)
            }

            override fun syncFailed(project: Project, errorMessage: String) {
                promise.setError(errorMessage)
            }
        }
        GradleSyncExecutor(project).sync(request, listener)

        return promise
    }
}