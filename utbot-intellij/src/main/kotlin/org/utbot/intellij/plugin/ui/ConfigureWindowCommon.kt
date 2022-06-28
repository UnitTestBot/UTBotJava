package org.utbot.intellij.plugin.ui

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModificationService
import com.intellij.openapi.ui.Messages
import org.jetbrains.concurrency.Promise
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.intellij.plugin.ui.utils.LibrarySearchScope
import org.utbot.intellij.plugin.ui.utils.findFrameworkLibrary
import org.utbot.intellij.plugin.ui.utils.parseVersion

fun createMockFrameworkNotificationDialog(title: String) = Messages.showYesNoDialog(
    """Mock framework ${MockFramework.MOCKITO.displayName} is not installed into current module. 
            |Would you like to install it now?""".trimMargin(),
    title,
    "Yes",
    "No",
    Messages.getQuestionIcon(),
)

fun configureMockFramework(project: Project, module: Module) {
    val selectedMockFramework = MockFramework.MOCKITO

    val libraryInProject =
        findFrameworkLibrary(project, module, selectedMockFramework, LibrarySearchScope.Project)
    val versionInProject = libraryInProject?.libraryName?.parseVersion()

    selectedMockFramework.isInstalled = true
    addDependency(project, module, mockitoCoreLibraryDescriptor(versionInProject))
        .onError { selectedMockFramework.isInstalled = false }
}

/**
 * Adds the dependency for selected framework via [JavaProjectModelModificationService].
 *
 * Note that version restrictions will be applied only if they are present on target machine
 * Otherwise latest release version will be installed.
 */
fun addDependency(project: Project, module: Module, libraryDescriptor: ExternalLibraryDescriptor): Promise<Void> {
    return JavaProjectModelModificationService
        .getInstance(project)
        //this method returns JetBrains internal Promise that is difficult to deal with, but it is our way
        .addDependency(module, libraryDescriptor, DependencyScope.TEST)
}
