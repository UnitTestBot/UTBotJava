package org.utbot.gradle.plugin.wrappers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.utbot.common.PathUtil.toPath
import java.nio.file.Paths

class SourceFindingStrategyGradleTest {

    @Nested
    @DisplayName("testsRelativePath")
    inner class TestsRelativePathTest {
        @Test
        fun `should relativize correctly`() {
            val projectRootPath = "./some/dir/"
            val testsRelativePath = "src/test.java"
            val testsAbsolutePath = "./some/dir/src/test.java"
            val sourceSetWrapperMock = createSourceSetWrapperMock(projectRootPath)

            val strategy = SourceFindingStrategyGradle(sourceSetWrapperMock, testsAbsolutePath)
            assertEquals(testsRelativePath.toPath(), strategy.testsRelativePath.toPath())
        }

        @Test
        fun `should return the file name if relativization failed`() {
            val projectRootPath = "./first/directory/"
            val testsRelativePath = "test.java" // just the file name
            val testsAbsolutePath = "./second/directory/src/test.java"
            val sourceSetWrapperMock = createSourceSetWrapperMock(projectRootPath)

            val strategy = SourceFindingStrategyGradle(sourceSetWrapperMock, testsAbsolutePath)
            assertEquals(testsRelativePath.toPath(), strategy.testsRelativePath.toPath())
        }
    }

    @Nested
    @DisplayName("getSourceRelativePath")
    inner class GetSourceRelativePathTest {
        @Test
        fun `should find and relativize the path correctly`() {
            val currentDirectory = "".toPath().toFile().absolutePath
            val sourceSetWrapperMock = createSourceSetWrapperMock(currentDirectory)
            val sourceFile = Paths.get(currentDirectory, "src", "com", "SomeClass.java").toFile()
            Mockito.`when`(sourceSetWrapperMock.findSourceCodeFile("com.SomeClass")).thenReturn(sourceFile)

            val strategy = SourceFindingStrategyGradle(sourceSetWrapperMock, testsFilePath = "will not be used")
            val sourcePathExpected = "src/com/SomeClass.java".toPath()
            val sourcePathActual = strategy.getSourceRelativePath("com.SomeClass", ".java")
            assertEquals(sourcePathExpected, sourcePathActual.toPath())
        }

        @Test
        fun `should return the path with the package if relativization failed`() {
            val projectRootPath = "./some/directory/"
            val sourceSetWrapperMock = createSourceSetWrapperMock(projectRootPath)
            val sourceFile = Paths.get("src", "com", "SomeClass.java").toFile()
            Mockito.`when`(sourceSetWrapperMock.findSourceCodeFile("com.SomeClass")).thenReturn(sourceFile)

            val strategy = SourceFindingStrategyGradle(sourceSetWrapperMock, testsFilePath = "will not be used")
            val sourcePathExpected = "com/SomeClass.java".toPath() // just the package with file name
            val sourcePathActual = strategy.getSourceRelativePath("com.SomeClass", ".java")
            assertEquals(sourcePathExpected, sourcePathActual.toPath())
        }
    }

    // internal

    private fun createSourceSetWrapperMock(projectRootPath: String): SourceSetWrapper {
        val sourceSetWrapperMock = Mockito.mock(SourceSetWrapper::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(sourceSetWrapperMock.parentProject.sarifProperties.projectRoot.absolutePath).thenReturn(projectRootPath)
        return sourceSetWrapperMock
    }
}