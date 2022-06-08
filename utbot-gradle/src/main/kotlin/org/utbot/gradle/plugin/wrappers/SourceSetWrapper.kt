package org.utbot.gradle.plugin.wrappers

import org.gradle.api.tasks.SourceSet
import org.utbot.common.FileUtil.createNewFileWithParentDirectories
import org.utbot.common.FileUtil.findAllFilesOnly
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.replaceSeparator
import org.utbot.common.tryLoadClass
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.sarif.util.ClassUtil
import org.utbot.framework.plugin.sarif.TargetClassWrapper
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

class SourceSetWrapper(
    val sourceSet: SourceSet,
    val parentProject: GradleProjectWrapper
) {

    /**
     * Runtime classpath of the [sourceSet].
     */
    val runtimeClasspath: String by lazy {
        sourceSet.runtimeClasspath.filter { file ->
            file.exists()
        }.joinToString(separator = ";") { file ->
            replaceSeparator(file.absolutePath)
        }
    }

    /**
     * ClassLoader that loaded classes from the [sourceSet].
     */
    val classLoader: URLClassLoader by lazy {
        val urls = sourceSet.runtimeClasspath.map { file ->
            file.toURI().toURL()
        }
        URLClassLoader(urls.toTypedArray())
    }

    /**
     * Absolute path to the build directory. For example, "./build/classes/java/main".
     */
    val workingDirectory: Path by lazy {
        sourceSet.output.classesDirs.first { directory ->
            directory.parentFile.name == "java" // only java is supported
        }.toPath()
    }

    /**
     * List of declared in the [sourceSet] classes.
     */
    val targetClasses: List<TargetClassWrapper> by lazy {
        parentProject.sarifProperties.targetClasses
            .ifEmpty {
                ClassUtil.findAllDeclaredClasses(classLoader, workingDirectory.toFile())
            }
            .mapNotNull { classFqn ->
                constructTargetClassWrapper(classFqn)
            }
    }

    /**
     * Finds the source code file by the given class fully qualified name.
     */
    fun findSourceCodeFile(classFqn: String): File? =
        ClassUtil.findSourceCodeFile(classFqn, sourceCodeFiles, classLoader)

    // internal

    /**
     * List of all source code files of this [sourceSet].
     */
    private val sourceCodeFiles: List<File> =
        sourceSet.java.srcDirs.flatMap { srcDir -> // only java is supported
            srcDir.findAllFilesOnly()
        }

    /**
     * Constructs [TargetClassWrapper] by the given [classFqn].
     */
    private fun constructTargetClassWrapper(classFqn: String): TargetClassWrapper? {
        val classUnderTest = classLoader.tryLoadClass(classFqn)?.kotlin
            ?: return null // `classFqn` may be defined not in this source set
        val sourceCodeFile = findSourceCodeFile(classFqn)
            ?: error("The source code for the class $classFqn was not found")
        return TargetClassWrapper(
            classFqn,
            classUnderTest,
            sourceCodeFile,
            createTestsCodeFile(classFqn),
            createSarifReportFile(classFqn)
        )
    }

    /**
     * Creates and returns a file for a future SARIF report.
     * For example, ".../main/com/qwerty/Main-utbot.sarif".
     */
    private fun createSarifReportFile(classFqn: String): File {
        val relativePath = "${sourceSet.name}/${classFqnToPath(classFqn)}-utbot.sarif"
        val absolutePath = Paths.get(parentProject.generatedSarifDirectory.path, relativePath)
        return absolutePath.toFile().apply { createNewFileWithParentDirectories() }
    }

    /**
     * Creates and returns a file for future generated tests.
     * For example, ".../java/main/com/qwerty/MainTest.java".
     */
    private fun createTestsCodeFile(classFqn: String): File {
        val fileExtension = parentProject.sarifProperties.codegenLanguage.extension
        val sourceRoot = parentProject.sarifProperties.codegenLanguage.toSourceRootName()
        val relativePath = "$sourceRoot/${sourceSet.name}/${classFqnToPath(classFqn)}Test$fileExtension"
        val absolutePath = Paths.get(parentProject.generatedTestsDirectory.path, relativePath)
        return absolutePath.toFile().apply { createNewFileWithParentDirectories() }
    }

    /**
     * Returns the source root name by [CodegenLanguage].
     */
    private fun CodegenLanguage.toSourceRootName(): String =
        when (this) {
            CodegenLanguage.JAVA -> "java"
            CodegenLanguage.KOTLIN -> "kotlin"
            else -> "unknown"
        }
}