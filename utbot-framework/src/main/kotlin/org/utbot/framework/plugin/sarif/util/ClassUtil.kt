package org.utbot.framework.plugin.sarif.util

import org.utbot.common.PathUtil
import org.utbot.common.loadClassesFromDirectory
import org.utbot.common.tryLoadClass
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.io.File

object ClassUtil {

    /**
     * Finds all classes loaded by the [classLoader] and whose class files
     * are located in the [classesDirectory]. Returns the names of the found classes.
     * Does not return classes without declared methods or without a canonicalName.
     */
    fun findAllDeclaredClasses(
        classLoader: ClassLoader,
        classesDirectory: File
    ): List<String> =
        classLoader
            .loadClassesFromDirectory(classesDirectory)
            .filter { clazz ->
                clazz.canonicalName != null && clazz.declaredMethods.isNotEmpty()
            }
            .map { clazz ->
                clazz.canonicalName
            }

    /**
     * Finds the source code file in the [sourceCodeFiles] by the given [classFqn].
     * Tries to find the file by the information available to [classLoader]
     * if the [classFqn] is not found in the [sourceCodeFiles].
     */
    fun findSourceCodeFile(
        classFqn: String,
        sourceCodeFiles: List<File>,
        classLoader: ClassLoader
    ): File? =
        sourceCodeFiles.firstOrNull { sourceCodeFile ->
            val relativePath = "${PathUtil.classFqnToPath(classFqn)}.${sourceCodeFile.extension}"
            sourceCodeFile.endsWith(File(relativePath))
        } ?: findSourceCodeFileByClass(classFqn, sourceCodeFiles, classLoader)

    // internal

    /**
     * Fallback logic: called after a failure of [findSourceCodeFile].
     */
    private fun findSourceCodeFileByClass(
        classFqn: String,
        sourceCodeFiles: List<File>,
        classLoader: ClassLoader
    ): File? {
        val clazz = classLoader.tryLoadClass(classFqn)
            ?: return null
        val sourceFileName = withUtContext(UtContext(classLoader)) {
            Instrumenter.computeSourceFileName(clazz) // finds the file name in bytecode
        } ?: return null
        val candidates = sourceCodeFiles.filter { sourceCodeFile ->
            sourceCodeFile.endsWith(File(sourceFileName))
        }
        return if (candidates.size == 1)
            candidates.first()
        else // we can't decide which file is needed
            null
    }
}