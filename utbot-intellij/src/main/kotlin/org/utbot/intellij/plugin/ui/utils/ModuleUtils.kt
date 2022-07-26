package org.utbot.intellij.plugin.ui.utils

import com.android.tools.idea.gradle.project.GradleProjectInfo
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
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.roots.TestModuleProperties
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.util.PathUtil.getParentPath
import java.nio.file.Path
import mu.KotlinLogging
import org.jetbrains.android.sdk.AndroidSdkType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.platform.TargetPlatformVersion

private val logger = KotlinLogging.logger {}

/**
 * @return jdk version of the module
 */
fun Module.jdkVersion(): JavaSdkVersion {
    val moduleRootManager = ModuleRootManager.getInstance(this)
    val sdk = moduleRootManager.sdk
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
fun Module.getOrCreateTestResourcesPath(testSourceRoot: VirtualFile?): Path {
    val testResourcesUrl = getOrCreateTestResourcesUrl(this, testSourceRoot)
    return VfsUtilCore.urlToPath(testResourcesUrl).toPath()
}

/**
 * Gets a path to Sarif reports directory or creates it.
 */
fun Module.getOrCreateSarifReportsPath(testSourceRoot: VirtualFile?): Path {
    val testResourcesPath = this.getOrCreateTestResourcesPath(testSourceRoot)
    return "$testResourcesPath/utbot-sarif-report/".toPath()
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
fun Module.suitableTestSourceRoots(codegenLanguage: CodegenLanguage): List<VirtualFile> {
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
        .filter { it.rootType == codegenLanguage.testRootType() }
        // Heuristics: User is more likely to choose the shorter path
        .sortedBy { it.url.length }
}
fun Project.isGradle() = GradleProjectInfo.getInstance(this).isBuildWithGradle

private const val dedicatedTestSourceRootName = "utbot_tests"
fun Module.addDedicatedTestRoot(testSourceRoots: MutableList<VirtualFile>): VirtualFile? {
    // Don't suggest new test source roots for Gradle project where 'unexpected' test roots won't work
    if (project.isGradle()) return null
    // Dedicated test root already exists
    if (testSourceRoots.any { file -> file.name == dedicatedTestSourceRootName }) return null

    val moduleInstance = ModuleRootManager.getInstance(this)
    val testFolder = moduleInstance.contentEntries.flatMap { it.sourceFolders.toList() }
        .firstOrNull { it.rootType in testSourceRootTypes }
    (testFolder?.let { testFolder.file?.parent } ?: (testFolder?.contentEntry
        ?: moduleInstance.contentEntries.first()).file ?: moduleFile)?.let {
        val file = FakeVirtualFile(it, dedicatedTestSourceRootName)
        testSourceRoots.add(file)
        // We return "true" IFF it's case of not yet created fake directory
        return if (VfsUtil.findRelativeFile(it, dedicatedTestSourceRootName) == null) file else null
    }
    return null
}

private const val resourcesSuffix = "/resources"

private fun getOrCreateTestResourcesUrl(module: Module, testSourceRoot: VirtualFile?): String {
    val rootModel = ModuleRootManager.getInstance(module).modifiableModel
    try {
        val sourceFolders = rootModel.contentEntries.flatMap { it.sourceFolders.toList() }

        val testResourcesFolder = sourceFolders
            .filter { sourceFolder ->
                sourceFolder.rootType in testResourceRootTypes && !sourceFolder.isForGeneratedSources()
            }
            // taking the source folder that has the maximum common prefix
            // with `testSourceRoot`, which was selected by the user
            .maxBy { sourceFolder ->
                val sourceFolderPath = sourceFolder.file?.path ?: ""
                val testSourceRootPath = testSourceRoot?.path ?: ""
                sourceFolderPath.commonPrefixWith(testSourceRootPath).length
            }
        if (testResourcesFolder != null) {
            return testResourcesFolder.url
        }

        val testFolder = sourceFolders.firstOrNull { it.rootType in testSourceRootTypes }
        val contentEntry = testFolder?.contentEntry ?: rootModel.contentEntries.first()

        val parentFolderUrl = testFolder?.let { getParentPath(testFolder.url) }
        val testResourcesUrl =
            if (parentFolderUrl != null) "${parentFolderUrl}$resourcesSuffix" else "${contentEntry.url}$resourcesSuffix"

        val codegenLanguage =
            if (testFolder?.rootType == TestResourceKotlinRootType) CodegenLanguage.KOTLIN else CodegenLanguage.JAVA

        try {
            contentEntry.addSourceRootIfAbsent(rootModel, testResourcesUrl, codegenLanguage.testResourcesRootType())
        } catch (e: java.lang.IllegalStateException) {
            // Hack to avoid unmodifiable ModuleBridge testModule on Android SAT-1536.
            workaround(WorkaroundReason.HACK) {
                logger.info("Error during SARIF report generation: $e")
                return testFolder!!.url
            }
        }

        return testResourcesUrl
    } finally {
        if (!rootModel.isDisposed && rootModel.isWritable) rootModel.dispose()
    }
}

fun ContentEntry.addSourceRootIfAbsent(
    model: ModifiableRootModel,
    sourceRootUrl: String,
    type: JpsModuleSourceRootType<*>
) {
    getSourceFolders(type).find { it.url == sourceRootUrl }?.apply {
        model.dispose()
        return
    }
    WriteCommandAction.runWriteCommandAction(rootModel.module.project) {
        try {
            VfsUtil.createDirectoryIfMissing(VfsUtilCore.urlToPath(sourceRootUrl))
            addSourceFolder(sourceRootUrl, type)
            model.commit()
        } catch (e: Exception) {
            logger.error { e }
            model.dispose()
        }
    }
}

/**
 * Obtain JDK version and make sure that it is JDK8 or JDK11
 */
private fun jdkVersionBy(sdk: Sdk?): JavaSdkVersion {
    if (sdk == null) {
        CommonErrorNotifier.notify("Failed to obtain JDK version of the project")
    }
    requireNotNull(sdk)

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
