package org.utbot.gradle.plugin.wrappers

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.utbot.gradle.plugin.buildProject
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import java.io.File

class GradleProjectWrapperTest {

    @Nested
    @DisplayName("childProjects")
    inner class ChildProjectsTest {
        @Test
        fun `should be empty by default`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            assertEquals(listOf<Project>(), gradleProject.childProjects)
        }
    }

    @Nested
    @DisplayName("sourceSets")
    inner class SourceSetsTest {
        @Test
        fun `should contain main source set`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            val mainSourceSet = gradleProject.sourceSets.firstOrNull { sourceSetWrapper ->
                sourceSetWrapper.sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME
            }
            assertNotNull(mainSourceSet)
        }

        @Test
        fun `should not contain test source set`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            val testSourceSet = gradleProject.sourceSets.firstOrNull { sourceSetWrapper ->
                sourceSetWrapper.sourceSet.name == SourceSet.TEST_SOURCE_SET_NAME
            }
            assertNull(testSourceSet)
        }
    }

    @Nested
    @DisplayName("generatedTestsDirectory")
    inner class GeneratedTestsDirectoryTest {
        @Test
        fun `should exist`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)

            val testsRelativePath = "generated/test"
            Mockito.`when`(sarifPropertiesMock.generatedTestsRelativeRoot).thenReturn(testsRelativePath)

            assert(gradleProject.generatedTestsDirectory.exists())
        }

        @Test
        fun `should end with the path from sarifProperties`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)

            val testsRelativePath = "generated/test"
            Mockito.`when`(sarifPropertiesMock.generatedTestsRelativeRoot).thenReturn(testsRelativePath)

            assert(gradleProject.generatedTestsDirectory.endsWith(File(testsRelativePath)))
        }
    }

    @Nested
    @DisplayName("generatedSarifDirectory")
    inner class GeneratedSarifDirectoryTest {
        @Test
        fun `should exist`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)

            val sarifRootRelativePath = "generated/sarif"
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn(sarifRootRelativePath)

            assert(gradleProject.generatedSarifDirectory.exists())
        }

        @Test
        fun `should end with the path from sarifProperties`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)

            val sarifRootRelativePath = "generated/sarif"
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn(sarifRootRelativePath)

            assert(gradleProject.generatedSarifDirectory.endsWith(File(sarifRootRelativePath)))
        }
    }

    @Nested
    @DisplayName("sarifReportFile")
    inner class SarifReportFileTest {
        @Test
        fun `should exist`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)

            val sarifRootRelativePath = "generated/sarif"
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn(sarifRootRelativePath)

            assert(gradleProject.sarifReportFile.exists())
        }

        @Test
        fun `should contain the project name`() {
            val project = buildProject()
            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)

            val sarifRootRelativePath = "generated/sarif"
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn(sarifRootRelativePath)

            assert(gradleProject.sarifReportFile.name.contains(project.name))
        }
    }

    // internal

    private val sarifPropertiesMock = Mockito.mock(SarifGradleExtensionProvider::class.java)
}