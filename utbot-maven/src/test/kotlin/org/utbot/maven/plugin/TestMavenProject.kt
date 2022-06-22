package org.utbot.maven.plugin

import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.FileUtils
import java.io.File
import java.io.FileReader
import java.nio.file.Path

/**
 * Wrapper for the maven project stored in the test resources.
 */
class TestMavenProject(pathToProject: Path) {

    /**
     * Path to the copied maven project.
     */
    val projectBaseDir =
        File("build/resources/${pathToProject.fileName}")

    init {
        projectBaseDir.deleteRecursively()
        // copying the project to the build directory to change it there
        FileUtils.copyDirectoryStructure(pathToProject.toFile(), projectBaseDir)
    }

    val mavenProject: MavenProject = run {
        val pomFile = File(projectBaseDir, "pom.xml")
        val model = MavenXpp3Reader().read(FileReader(pomFile))
        val mavenProject = MavenProject(model)
        mavenProject.setPomFile(pomFile)
        mavenProject.collectedProjects = listOf() // no child modules
        mavenProject.addCompileSourceRoot(mavenProject.build.sourceDirectory)
        FieldUtils.writeField(mavenProject, "basedir", projectBaseDir, true)
        mavenProject
    }
}