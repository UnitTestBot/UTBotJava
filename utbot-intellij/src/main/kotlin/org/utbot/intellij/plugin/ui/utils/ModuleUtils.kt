package org.utbot.intellij.plugin.ui.utils

import org.utbot.common.PathUtil.toPath
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.intellij.plugin.ui.CommonErrorNotifier
import org.utbot.intellij.plugin.ui.UnsupportedJdkNotifier
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.roots.TestModuleProperties
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil.getParentPath
import java.nio.file.Path
import mu.KotlinLogging
import org.jetbrains.android.sdk.AndroidSdkType
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.platform.TargetPlatformVersion

private val logger = KotlinLogging.logger {}

/**
 * @return jdk version of the module
 */
fun Module.jdkVersion(): JavaSdkVersion {
    val moduleRootManager = ModuleRootManager.getInstance(this)
    val sdk = moduleRootManager.sdk ?: error("No sdk found for module $this") // TODO: get sdk from project?
    return jdkVersionBy(sdk)
}

/**
 * @return jvm target version of the module
 */
fun Module.kotlinTargetPlatform(): TargetPlatformVersion {
    val facetSettingsProvider = KotlinFacetSettingsProvider.getInstance(this.project)
        ?: error("No facet settings for module $this")
    val moduleFacetSettings = facetSettingsProvider.getInitializedSettings(this)

    return moduleFacetSettings.targetPlatform
        ?.componentPlatforms
        ?.map { it.targetPlatformVersion }
        ?.singleOrNull() ?: error("Can't determine target platform for module $this")
}

fun Module.suitableTestSourceRoots(): List<VirtualFile> =
    suitableTestSourceRoots(CodegenLanguage.JAVA) + suitableTestSourceRoots(CodegenLanguage.KOTLIN)

fun Module.suitableTestSourceFolders(): List<SourceFolder> =
    suitableTestSourceFolders(CodegenLanguage.JAVA) + suitableTestSourceFolders(CodegenLanguage.KOTLIN)

/**
 * Gets a path to test resources source root.
 *
 * If no roots exist, our suggestion is a folder named "resources" in the entry root.
 */
fun Module.getOrCreateTestResourcesPath(): Path {
    val testResourcesUrl = getOrCreateTestResourcesUrl(this)
    return VfsUtilCore.urlToPath(testResourcesUrl).toPath()
}

/**
 * Gets a path to Sarif reports directory or creates it.
 *
 */
fun Module.getOrCreateSarifReportsPath(): Path {
    val testResourcesPath = this.getOrCreateTestResourcesPath()
    return "$testResourcesPath/sarif/".toPath()
}

/**
 * Find test module by current source module.
 */
fun Module.testModule(project: Project): Module {
    var testModule = findPotentialModuleForTests(project, this)
    val testRootUrls = testModule.suitableTestSourceRoots()

    //if no suitable module for tests is found, create tests in the same root
    if (testRootUrls.isEmpty() && testModule.suitableTestSourceFolders().isEmpty()) {
        testModule = this
    }
    return testModule
}

private fun findPotentialModuleForTests(project: Project, srcModule: Module): Module {
    for (module in ModuleManager.getInstance(project).modules) {
        if (srcModule == TestModuleProperties.getInstance(module).productionModule) {
            return module
        }
    }

    if (srcModule.suitableTestSourceFolders().isEmpty()) {
        val modules = mutableSetOf<Module>()
        ModuleUtilCore.collectModulesDependsOn(srcModule, modules)
        modules.remove(srcModule)

        val modulesWithTestRoot = modules.filter { it.suitableTestSourceFolders().isNotEmpty() }
        if (modulesWithTestRoot.size == 1) return modulesWithTestRoot[0]
    }
    return srcModule
}

/**
 * Finds all suitable test root virtual files.
 */
private fun Module.suitableTestSourceRoots(codegenLanguage: CodegenLanguage): List<VirtualFile> {
    val sourceRootsInModule = suitableTestSourceFolders(codegenLanguage).mapNotNull { it.file }

    if (sourceRootsInModule.isNotEmpty()) {
        return sourceRootsInModule
    }

    //suggest choosing from all dependencies modules
    val dependentModules = mutableSetOf<Module>()
    ModuleUtilCore.collectModulesDependsOn(this, dependentModules)

    return dependentModules
        .flatMap { it.suitableTestSourceFolders(codegenLanguage) }
        .mapNotNull { it.file }
}

private fun Module.suitableTestSourceFolders(codegenLanguage: CodegenLanguage): List<SourceFolder> {
    val sourceFolders = ModuleRootManager.getInstance(this)
        .contentEntries
        .flatMap { it.sourceFolders.toList() }
        .filterNotNull()

    return sourceFolders
        .filterNot { it.isForGeneratedSources() }
        .filter { folder -> folder.rootType == codegenLanguage.testRootType() }
}

private const val resourcesSuffix = "/resources"

private fun getOrCreateTestResourcesUrl(module: Module): String {
    val moduleInstance = ModuleRootManager.getInstance(module)
    val sourceFolders = moduleInstance.contentEntries.flatMap { it.sourceFolders.toList() }

    val testResourcesFolder = sourceFolders.firstOrNull { it.rootType in testResourceRootTypes }
    if (testResourcesFolder != null) {
        return testResourcesFolder.url
    }

    val testFolder = sourceFolders.firstOrNull { f -> f.rootType in testSourceRootTypes }
    val contentEntry = testFolder?.contentEntry ?: moduleInstance.contentEntries.first()

    val parentFolderUrl = testFolder?.let { getParentPath(testFolder.url) }
    val testResourcesUrl =
        if (parentFolderUrl != null) "${parentFolderUrl}$resourcesSuffix" else "${contentEntry.url}$resourcesSuffix"

    val codegenLanguage =
        if (testFolder?.rootType == TestResourceKotlinRootType) CodegenLanguage.KOTLIN else CodegenLanguage.JAVA

    try {
        WriteCommandAction.runWriteCommandAction(module.project) {
            contentEntry.addSourceFolder(testResourcesUrl, codegenLanguage.testResourcesRootType())
            moduleInstance.modifiableModel.commit()
            VfsUtil.createDirectoryIfMissing(VfsUtilCore.urlToPath(testResourcesUrl))
        }
    }
    catch (e: java.lang.IllegalStateException) {
        // Hack to avoid unmodifiable ModuleBridge testModule on Android SAT-1536.
        workaround(WorkaroundReason.HACK) {
            logger.info("Error during SARIF report generation: $e")
            return testFolder!!.url
        }
    }

    return testResourcesUrl
}

/**
 * Obtain JDK version and make sure that it is JDK8 or JDK11
 */
private fun jdkVersionBy(sdk: Sdk): JavaSdkVersion {
    val jdkVersion = when (sdk.sdkType) {
        is JavaSdk -> {
            (sdk.sdkType as JavaSdk).getVersion(sdk)
        }
        is AndroidSdkType -> {
            ((sdk.sdkType as AndroidSdkType).dependencyType as JavaSdk).getVersion(sdk)
        }
        else -> null
    }
    if (jdkVersion == null) {
        CommonErrorNotifier.notify("Failed to obtain JDK version of the project")
    }
    requireNotNull(jdkVersion)
    if (!jdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
        UnsupportedJdkNotifier.notify(jdkVersion.description)
    }
    return jdkVersion
}
