package org.utbot.intellij.plugin.ui.utils

import org.utbot.common.PathUtil.toPath
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.framework.plugin.api.CodegenLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
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
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.platform.TargetPlatformVersion

private val logger = KotlinLogging.logger {}

interface ITestSourceRoot {
    val dirPath: String
    val dirName: String
    val dir: VirtualFile?
    val expectedLanguage : CodegenLanguage
}

class TestSourceRoot(override val dir: VirtualFile, override val expectedLanguage: CodegenLanguage) : ITestSourceRoot {
    override val dirPath: String = dir.toNioPath().toString()
    override val dirName: String = dir.name

    override fun toString() = dirPath

    override fun equals(other: Any?) =
        other is TestSourceRoot && dir == other.dir && expectedLanguage == other.expectedLanguage

    override fun hashCode() = 31 * dir.hashCode() + expectedLanguage.hashCode()
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

/**
 * Gets paths to project resources source roots.
 *
 * E.g. src/main/resources
 */
fun Module.getResourcesPaths(): List<Path> =
    ModuleRootManager.getInstance(this)
        .contentEntries
        .flatMap { it.sourceFolders.toList() }
        .filter { it.rootType is JavaResourceRootType && !it.isTestSource }
        .mapNotNull { it.file?.toNioPath() }

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
 * Find test modules by current source module.
 */
fun Module.testModules(project: Project): List<Module> {
    var testModules = findPotentialModulesForTests(project, this)
    val testRootUrls = testModules.flatMap { it.suitableTestSourceRoots() }

    //if no suitable module for tests is found, create tests in the same root
    if (testRootUrls.isEmpty() && testModules.flatMap { it.suitableTestSourceFolders() }.isEmpty()) {
        testModules = listOf(this)
    }
    return testModules
}

private fun findPotentialModulesForTests(project: Project, srcModule: Module): List<Module> {
    val modules = mutableListOf<Module>()
    for (module in ModuleManager.getInstance(project).modules) {
        if (srcModule == TestModuleProperties.getInstance(module).productionModule) {
            modules += module
        }
    }
    if (modules.isNotEmpty()) return modules

    if (srcModule.suitableTestSourceFolders().isEmpty()) {
        val modulesWithTestRoot = mutableSetOf<Module>().also {
            ModuleUtilCore.collectModulesDependsOn(srcModule, it)
            it.remove(srcModule)
        }.filter { it.suitableTestSourceFolders().isNotEmpty() }

        if (modulesWithTestRoot.size == 1) return modulesWithTestRoot
    }
    return listOf(srcModule)
}

/**
 * Finds all suitable test root virtual files.
 */
fun Module.suitableTestSourceRoots(): List<TestSourceRoot> {
    val sourceRootsInModule = suitableTestSourceFolders().mapNotNull { it.testSourceRoot }

    if (sourceRootsInModule.isNotEmpty()) {
        return sourceRootsInModule
    }

    //suggest choosing from all dependencies modules
    val dependentModules = mutableSetOf<Module>()
    ModuleUtilCore.collectModulesDependsOn(this, dependentModules)

    return dependentModules
        .flatMap { it.suitableTestSourceFolders() }
        .mapNotNull { it.testSourceRoot }
}

private val SourceFolder.testSourceRoot:TestSourceRoot?
    get() {
        val file = file
        val expectedLanguage = expectedLanguageForTests
        if (file != null && expectedLanguage != null)
            return TestSourceRoot(file, expectedLanguage)
        return null
    }

private fun Module.suitableTestSourceFolders(): List<SourceFolder> {
    val sourceFolders = ModuleRootManager.getInstance(this)
        .contentEntries
        .flatMap { it.sourceFolders.toList() }
        .filterNotNull()

    return sourceFolders
        .filterNot { it.isForGeneratedSources() }
        .filter { it.isTestSource }
}

private val GRADLE_SYSTEM_ID = ProjectSystemId("GRADLE")

val Project.isBuildWithGradle get() =
    ModuleManager.getInstance(this).modules.any {
        ExternalSystemApiUtil.isExternalSystemAwareModule(GRADLE_SYSTEM_ID, it)
    }

const val dedicatedTestSourceRootName = "utbot_tests"

fun Module.addDedicatedTestRoot(testSourceRoots: MutableList<ITestSourceRoot>, language: CodegenLanguage): VirtualFile? {
    // Don't suggest new test source roots for a Gradle project where 'unexpected' test roots won't work
    if (project.isBuildWithGradle) return null
    // Dedicated test root already exists
    if (testSourceRoots.any { root -> root.dir?.name == dedicatedTestSourceRootName }) return null

    val moduleInstance = ModuleRootManager.getInstance(this)
    val testFolder = moduleInstance.contentEntries
        .flatMap { it.sourceFolders.toList() }
        .filterNot { it.isForGeneratedSources() }
        .firstOrNull { it.rootType in testSourceRootTypes }

    (testFolder?.let { testFolder.file?.parent }
        ?: testFolder?.contentEntry?.file ?: this.guessModuleDir())?.let {
        val file = FakeVirtualFile(it, dedicatedTestSourceRootName)
        testSourceRoots.add(TestSourceRoot(file, language))
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
            .maxByOrNull { sourceFolder ->
                val sourceFolderPath = sourceFolder.file?.path ?: ""
                val testSourceRootPath = testSourceRoot?.path ?: ""
                sourceFolderPath.commonPrefixWith(testSourceRootPath).length
            }
        if (testResourcesFolder != null) {
            return testResourcesFolder.url
        }

        val testFolder = sourceFolders.firstOrNull { it.rootType in testSourceRootTypes }
        val contentEntry = testFolder?.getModifiableContentEntry() ?: rootModel.contentEntries.first()

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

private fun SourceFolder.getModifiableContentEntry() : ContentEntry? {
    return ModuleRootManager.getInstance(contentEntry.rootModel.module).modifiableModel.contentEntries.find { entry -> entry.url == url }
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

private val SourceFolder.expectedLanguageForTests: CodegenLanguage?
    get() {
        // unfortunately, Gradle creates Kotlin test source root with Java source root type, so type is misleading,
        // and we should try looking for name first
        if (file?.name == "kotlin")
            return CodegenLanguage.KOTLIN

        if (file?.name == "java")
            return CodegenLanguage.JAVA

        return when (rootType) {
            CodegenLanguage.KOTLIN.testRootType() -> CodegenLanguage.KOTLIN
            CodegenLanguage.JAVA.testRootType() -> CodegenLanguage.JAVA
            else -> null
        }
    }