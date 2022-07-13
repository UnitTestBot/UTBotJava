package org.utbot.maven.plugin.wrappers

import org.apache.maven.project.MavenProject
import org.utbot.common.FileUtil.createNewFileWithParentDirectories
import org.utbot.common.FileUtil.findAllFilesOnly
import org.utbot.common.PathUtil
import org.utbot.common.PathUtil.toPath
import org.utbot.common.tryLoadClass
import org.utbot.framework.plugin.sarif.util.ClassUtil
import org.utbot.framework.plugin.sarif.TargetClassWrapper
import org.utbot.maven.plugin.extension.SarifMavenConfigurationProvider
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Contains information about the maven project for which we are creating a SARIF report.
 */
class MavenProjectWrapper(
    val mavenProject: MavenProject,
    val sarifProperties: SarifMavenConfigurationProvider
) {

    /**
     * Contains child projects of the [mavenProject].
     */
    val childProjects: List<MavenProjectWrapper> by lazy {
        mavenProject.collectedProjects.map { childProject ->
            MavenProjectWrapper(childProject, sarifProperties)
        }
    }

    /**
     * Directory for generated tests. For example, "build/generated/test/".
     */
    val generatedTestsDirectory: File by lazy {
        mavenProject.basedir.resolve(sarifProperties.generatedTestsRelativeRoot).apply { mkdirs() }
    }

    /**
     * Directory for created SARIF reports. For example, "build/generated/sarif/".
     */
    val generatedSarifDirectory: File by lazy {
        mavenProject.basedir.resolve(sarifProperties.sarifReportsRelativeRoot).apply { mkdirs() }
    }

    /**
     * SARIF report file containing results from all others reports from the [mavenProject].
     */
    val sarifReportFile: File by lazy {
        Paths.get(
            generatedSarifDirectory.path,
            "${mavenProject.name}Report.sarif"
        ).toFile().apply {
            createNewFileWithParentDirectories()
        }
    }

    /**
     * Runtime classpath of the [mavenProject].
     */
    val runtimeClasspath: String by lazy {
        mavenProject.runtimeClasspathElements.joinToString(File.pathSeparator)
    }

    /**
     * ClassLoader that loaded classes from the [mavenProject].
     */
    val classLoader: URLClassLoader by lazy {
        val urls = mavenProject.runtimeClasspathElements.map { path ->
            path.toPath().toUri().toURL()
        }
        URLClassLoader(urls.toTypedArray())
    }

    /**
     * Absolute path to the build directory. For example, "./target/classes".
     */
    val workingDirectory: Path by lazy {
        mavenProject.build.outputDirectory.toPath()
    }

    /**
     * List of declared in the [mavenProject] classes.
     * Does not contain classes without declared methods or without a canonicalName.
     */
    val targetClasses: List<TargetClassWrapper> by lazy {
        sarifProperties.targetClasses
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
     * List of all source code files of this [mavenProject].
     */
    private val sourceCodeFiles: List<File> by lazy {
        mavenProject.compileSourceRoots.flatMap { srcDir ->
            srcDir.toPath().toFile().findAllFilesOnly()
        }
    }

    /**
     * Constructs [TargetClassWrapper] by the given [classFqn].
     */
    private fun constructTargetClassWrapper(classFqn: String): TargetClassWrapper? {
        val classUnderTest = classLoader.tryLoadClass(classFqn)?.kotlin
            ?: return null // `classFqn` may be defined not in this module
        val sourceCodeFile = findSourceCodeFile(classFqn)
            ?: error("The source code for the class $classFqn was not found")
        return TargetClassWrapper(
            classFqn,
            classUnderTest,
            sourceCodeFile,
            createTestsCodeFile(classFqn),
            createSarifReportFile(classFqn),
            sarifProperties.testPrivateMethods
        )
    }

    /**
     * Creates and returns a file for a future SARIF report.
     * For example, ".../main/com/qwerty/MainReport.sarif".
     */
    private fun createSarifReportFile(classFqn: String): File {
        val relativePath = "${PathUtil.classFqnToPath(classFqn)}Report.sarif"
        val absolutePath = Paths.get(generatedSarifDirectory.path, relativePath)
        return absolutePath.toFile().apply { createNewFileWithParentDirectories() }
    }

    /**
     * Creates and returns a file for future generated tests.
     * For example, ".../com/qwerty/MainTest.java".
     */
    private fun createTestsCodeFile(classFqn: String): File {
        val fileExtension = sarifProperties.codegenLanguage.extension
        val relativePath = "${PathUtil.classFqnToPath(classFqn)}Test$fileExtension"
        val absolutePath = Paths.get(generatedTestsDirectory.path, relativePath)
        return absolutePath.toFile().apply { createNewFileWithParentDirectories() }
    }
}