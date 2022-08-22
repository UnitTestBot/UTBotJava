package org.utbot.gradle.plugin.wrappers

import org.utbot.common.PathUtil
import org.utbot.common.PathUtil.toPath
import org.utbot.sarif.SourceFindingStrategy
import java.io.File

/**
 * The search strategy based on the information available to the Gradle.
 *
 * @param sourceSet the source set which contains all needed source files.
 * @param testsFilePath absolute path to the file containing generated tests
 *                      for the class for which we are creating the SARIF report.
 * @param defaultExtension the file extension to be used in [getSourceRelativePath]
 *                         if the source file was not found by the class qualified name
 *                         and the `extension` parameter is null.
 */
class SourceFindingStrategyGradle(
    private val sourceSet: SourceSetWrapper,
    private val testsFilePath: String,
    private val defaultExtension: String = ".java"
) : SourceFindingStrategy() {

    /**
     * Returns the relative path (against [projectRootPath]) to the file with generated tests.
     */
    override val testsRelativePath: String
        get() = PathUtil.safeRelativize(projectRootPath, testsFilePath)
            ?: testsFilePath.toPath().fileName.toString()

    /**
     * Returns the relative path (against [projectRootPath]) to the source file containing the class [classFqn].
     */
    override fun getSourceRelativePath(classFqn: String, extension: String?): String {
        val defaultPath = PathUtil.classFqnToPath(classFqn) + (extension ?: defaultExtension)
        return sourceSet.findSourceCodeFile(classFqn)?.let { sourceCodeFile ->
            PathUtil.safeRelativize(projectRootPath, sourceCodeFile.absolutePath)
        } ?: defaultPath
    }

    /**
     * Finds the source file containing the class [classFqn].
     * Returns null if the file does not exist.
     */
    override fun getSourceFile(classFqn: String, extension: String?): File? =
        sourceSet.findSourceCodeFile(classFqn)

    // internal

    private val projectRootPath = sourceSet.parentProject.sarifProperties.projectRoot.absolutePath
}