package org.utbot.sarif

import org.utbot.common.PathUtil
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.fileExtension
import org.utbot.common.PathUtil.toPath
import java.io.File

/**
 * Defines the search strategy for the source files. Used when creating a SARIF report.
 * All paths should be relative to the project root so that the links in the report are shown correctly.
 */
abstract class SourceFindingStrategy {

    /**
     * Returns a path to the file with generated tests.
     */
    abstract val testsRelativePath: String

    /**
     * Returns a path to the source file by given [classFqn].
     */
    abstract fun getSourceRelativePath(classFqn: String, extension: String? = null): String

    /**
     * Returns the source file by given [classFqn].
     */
    abstract fun getSourceFile(classFqn: String, extension: String? = null): File?
}

/**
 * This implementation of the SourceFindingStrategy tries to construct paths
 * to source files with zero knowledge of the actual structure of the project.
 * So it assumes that project has predictable (standard) structure.
 * Otherwise, it may not work properly.
 *
 * @param sourceClassFqn fully qualified name of the class for which we are creating the report
 * @param sourceFilePath absolute path to the file containing `sourceClassFqn`
 * @param testsFilePath absolute path to the file containing generated tests for the `sourceClassFqn`
 * @param projectRootPath absolute path to the root of the relative paths in the SARIF report
 */
class SourceFindingStrategyDefault(
    sourceClassFqn: String,
    sourceFilePath: String,
    testsFilePath: String,
    private val projectRootPath: String
) : SourceFindingStrategy() {

    /**
     * Tries to construct the relative path to tests (against `projectRootPath`) using the `testsFilePath`.
     */
    override val testsRelativePath =
        PathUtil.safeRelativize(projectRootPath, testsFilePath) ?: testsFilePath.toPath().fileName.toString()

    /**
     * Tries to guess the relative path (against `projectRootPath`) to the source file containing the class [classFqn].
     */
    override fun getSourceRelativePath(classFqn: String, extension: String?): String {
        val fileExtension = extension ?: sourceExtension
        val absolutePath = resolveClassFqn(sourceFilesDirectory, classFqn, fileExtension)
        val relativePath = PathUtil.safeRelativize(projectRootPath, absolutePath)
        return relativePath ?: (classFqnToPath(classFqn) + fileExtension)
    }

    /**
     * Tries to find the source file containing the class [classFqn].
     * Returns null if the file does not exist.
     */
    override fun getSourceFile(classFqn: String, extension: String?): File? {
        val fileExtension = extension ?: sourceExtension
        val absolutePath = resolveClassFqn(sourceFilesDirectory, classFqn, fileExtension)
        return absolutePath?.let(::File)
    }

    // internal

    private val sourceExtension = sourceFilePath.toPath().fileExtension

    private val sourceFilesDirectory =
        PathUtil.removeClassFqnFromPath(sourceFilePath, sourceClassFqn)

    /**
     * Resolves [classFqn] against [absolutePath] and checks if a resolved path exists.
     *
     * Example: resolveClassFqn("C:/project/src/", "com.Main") = "C:/project/src/com/Main.java".
     */
    private fun resolveClassFqn(absolutePath: String?, classFqn: String, extension: String = ".java"): String? {
        if (absolutePath == null)
            return null
        val toResolve = classFqnToPath(classFqn) + extension
        return PathUtil.resolveIfExists(absolutePath, toResolve)
    }
}