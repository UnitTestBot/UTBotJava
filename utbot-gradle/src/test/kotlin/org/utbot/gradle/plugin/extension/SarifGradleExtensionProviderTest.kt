package org.utbot.gradle.plugin.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.utbot.common.PathUtil.toPath
import org.utbot.engine.Mocker
import org.utbot.framework.codegen.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.gradle.plugin.buildProject
import java.io.File

class SarifGradleExtensionProviderTest {

    @Nested
    @DisplayName("targetClasses")
    inner class TargetClassesTest {
        @Test
        fun `should be an empty list by default`() {
            setTargetClasses(null)
            assertEquals(listOf<String>(), extensionProvider.targetClasses)
        }

        @Test
        fun `should be provided from the extension`() {
            val targetClasses = listOf("com.abc.Main")
            setTargetClasses(targetClasses)
            assertEquals(targetClasses, extensionProvider.targetClasses)
        }

        private fun setTargetClasses(value: List<String>?) =
            Mockito.`when`(extensionMock.targetClasses).thenReturn(createListProperty(value))
    }

    @Nested
    @DisplayName("projectRoot")
    inner class ProjectRootTest {
        @Test
        fun `should be projectDir by default`() {
            setProjectRoot(null)
            assertEquals(project.projectDir, extensionProvider.projectRoot)
        }

        @Test
        fun `should be provided from the extension`() {
            val projectRoot = "some/dir/"
            setProjectRoot(projectRoot)
            assertEquals(File(projectRoot), extensionProvider.projectRoot)
        }

        private fun setProjectRoot(value: String?) =
            Mockito.`when`(extensionMock.projectRoot).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("generatedTestsRelativeRoot")
    inner class GeneratedTestsRelativeRootTest {
        @Test
        fun `should be build generated test by default`() {
            val testsRootExpected = "build/generated/test"
            setGeneratedTestsRelativeRoot(null)
            assertEquals(testsRootExpected.toPath(), extensionProvider.generatedTestsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the extension`() {
            val testsRoot = "some/dir/"
            setGeneratedTestsRelativeRoot(testsRoot)
            assertEquals(testsRoot.toPath(), extensionProvider.generatedTestsRelativeRoot.toPath())
        }

        private fun setGeneratedTestsRelativeRoot(value: String?) =
            Mockito.`when`(extensionMock.generatedTestsRelativeRoot).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("sarifReportsRelativeRoot")
    inner class SarifReportsRelativeRootTest {
        @Test
        fun `should be build generated sarif by default`() {
            setSarifReportsRelativeRoot(null)
            val sarifRoot = "build/generated/sarif"
            assertEquals(sarifRoot.toPath(), extensionProvider.sarifReportsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the extension`() {
            val sarifRoot = "some/dir/"
            setSarifReportsRelativeRoot(sarifRoot)
            assertEquals(sarifRoot.toPath(), extensionProvider.sarifReportsRelativeRoot.toPath())
        }

        private fun setSarifReportsRelativeRoot(value: String?) =
            Mockito.`when`(extensionMock.sarifReportsRelativeRoot).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("markGeneratedTestsDirectoryAsTestSourcesRoot")
    inner class MarkGeneratedTestsDirectoryAsTestSourcesRootTest {
        @Test
        fun `should be true by default`() {
            setMark(null)
            assertEquals(true, extensionProvider.markGeneratedTestsDirectoryAsTestSourcesRoot)
        }

        @Test
        fun `should be provided from the extension`() {
            setMark(false)
            assertEquals(false, extensionProvider.markGeneratedTestsDirectoryAsTestSourcesRoot)
        }

        private fun setMark(value: Boolean?) =
            Mockito.`when`(extensionMock.markGeneratedTestsDirectoryAsTestSourcesRoot).thenReturn(createBooleanProperty(value))
    }

    @Nested
    @DisplayName("testFramework")
    inner class TestFrameworkTest {
        @Test
        fun `should be TestFramework defaultItem by default`() {
            setTestFramework(null)
            assertEquals(TestFramework.defaultItem, extensionProvider.testFramework)
        }

        @Test
        fun `should be equal to Junit4`() {
            setTestFramework("junit4")
            assertEquals(Junit4, extensionProvider.testFramework)
        }

        @Test
        fun `should be equal to Junit5`() {
            setTestFramework("junit5")
            assertEquals(Junit5, extensionProvider.testFramework)
        }

        @Test
        fun `should be equal to TestNg`() {
            setTestFramework("testng")
            assertEquals(TestNg, extensionProvider.testFramework)
        }

        @Test
        fun `should fail on unknown test framework`() {
            setTestFramework("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.testFramework
            }
        }

        private fun setTestFramework(value: String?) =
            Mockito.`when`(extensionMock.testFramework).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("mockFramework")
    inner class MockFrameworkTest {
        @Test
        fun `should be MockFramework defaultItem by default`() {
            setMockFramework(null)
            assertEquals(MockFramework.defaultItem, extensionProvider.mockFramework)
        }

        @Test
        fun `should be equal to MOCKITO`() {
            setMockFramework("mockito")
            assertEquals(MockFramework.MOCKITO, extensionProvider.mockFramework)
        }

        @Test
        fun `should fail on unknown mock framework`() {
            setMockFramework("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.mockFramework
            }
        }

        private fun setMockFramework(value: String?) =
            Mockito.`when`(extensionMock.mockFramework).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("generationTimeout")
    inner class GenerationTimeoutTest {
        @Test
        fun `should be 60 seconds by default`() {
            setGenerationTimeout(null)
            assertEquals(60 * 1000L, extensionProvider.generationTimeout)
        }

        @Test
        fun `should be provided from the extension`() {
            setGenerationTimeout(100L)
            assertEquals(100L, extensionProvider.generationTimeout)
        }

        @Test
        fun `should fail on negative timeout`() {
            setGenerationTimeout(-1)
            assertThrows<IllegalStateException> {
                extensionProvider.generationTimeout
            }
        }

        private fun setGenerationTimeout(value: Long?) =
            Mockito.`when`(extensionMock.generationTimeout).thenReturn(createLongProperty(value))
    }

    @Nested
    @DisplayName("codegenLanguage")
    inner class CodegenLanguageTest {
        @Test
        fun `should be CodegenLanguage defaultItem by default`() {
            setCodegenLanguage(null)
            assertEquals(CodegenLanguage.defaultItem, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should be equal to JAVA`() {
            setCodegenLanguage("java")
            assertEquals(CodegenLanguage.JAVA, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should be equal to KOTLIN`() {
            setCodegenLanguage("kotlin")
            assertEquals(CodegenLanguage.KOTLIN, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should fail on unknown codegen language`() {
            setCodegenLanguage("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.codegenLanguage
            }
        }

        private fun setCodegenLanguage(value: String?) =
            Mockito.`when`(extensionMock.codegenLanguage).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("mockStrategy")
    inner class MockStrategyTest {
        @Test
        fun `should be MockStrategyApi defaultItem by default`() {
            setMockStrategy(null)
            assertEquals(MockStrategyApi.defaultItem, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be equal to NO_MOCKS`() {
            setMockStrategy("do-not-mock")
            assertEquals(MockStrategyApi.NO_MOCKS, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be equal to OTHER_PACKAGES`() {
            setMockStrategy("package-based")
            assertEquals(MockStrategyApi.OTHER_PACKAGES, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be equal to OTHER_CLASSES`() {
            setMockStrategy("all-except-cut")
            assertEquals(MockStrategyApi.OTHER_CLASSES, extensionProvider.mockStrategy)
        }

        @Test
        fun `should fail on unknown mock strategy`() {
            setMockStrategy("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.mockStrategy
            }
        }

        private fun setMockStrategy(value: String?) =
            Mockito.`when`(extensionMock.mockStrategy).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("staticsMocking")
    inner class StaticsMockingTest {
        @Test
        fun `should be StaticsMocking defaultItem by default`() {
            setStaticsMocking(null)
            assertEquals(StaticsMocking.defaultItem, extensionProvider.staticsMocking)
        }

        @Test
        fun `should be equal to NoStaticMocking`() {
            setStaticsMocking("do-not-mock-statics")
            assertEquals(NoStaticMocking, extensionProvider.staticsMocking)
        }

        @Test
        fun `should be equal to`() {
            setStaticsMocking("mock-statics")
            assertEquals(MockitoStaticMocking, extensionProvider.staticsMocking)
        }

        @Test
        fun `should fail on unknown statics mocking`() {
            setStaticsMocking("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.staticsMocking
            }
        }

        private fun setStaticsMocking(value: String?) =
            Mockito.`when`(extensionMock.staticsMocking).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("forceStaticMocking")
    inner class ForceStaticMockingTest {
        @Test
        fun `should be ForceStaticMocking defaultItem by default`() {
            setForceStaticMocking(null)
            assertEquals(ForceStaticMocking.defaultItem, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should be equal to FORCE`() {
            setForceStaticMocking("force")
            assertEquals(ForceStaticMocking.FORCE, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should be equal to DO_NOT_FORCE`() {
            setForceStaticMocking("do-not-force")
            assertEquals(ForceStaticMocking.DO_NOT_FORCE, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should fail on unknown force static mocking`() {
            setForceStaticMocking("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.forceStaticMocking
            }
        }

        private fun setForceStaticMocking(value: String?) =
            Mockito.`when`(extensionMock.forceStaticMocking).thenReturn(createStringProperty(value))
    }

    @Nested
    @DisplayName("classesToMockAlways")
    inner class ClassesToMockAlwaysTest {

        private val defaultClasses =
            Mocker.defaultSuperClassesToMockAlwaysNames.map(::ClassId).toSet()

        @Test
        fun `should be defaultSuperClassesToMockAlwaysNames by default`() {
            setClassesToMockAlways(null)
            assertEquals(defaultClasses, extensionProvider.classesToMockAlways)
        }

        @Test
        fun `should be provided from the extension`() {
            val classes = listOf("com.abc.Main")
            val expectedClasses = classes.map(::ClassId).toSet() + defaultClasses
            setClassesToMockAlways(classes)
            assertEquals(expectedClasses, extensionProvider.classesToMockAlways)
        }

        private fun setClassesToMockAlways(value: List<String>?) =
            Mockito.`when`(extensionMock.classesToMockAlways).thenReturn(createListProperty(value))
    }

    // internal

    private val project = buildProject()
    private val extensionMock = Mockito.mock(SarifGradleExtension::class.java)
    private val extensionProvider = SarifGradleExtensionProvider(project, extensionMock)

    // properties

    private fun createBooleanProperty(value: Boolean?) =
        project.objects.property(Boolean::class.java).apply {
            set(value)
        }

    private fun createLongProperty(value: Long?) =
        project.objects.property(Long::class.java).apply {
            set(value)
        }

    private fun createStringProperty(value: String?) =
        project.objects.property(String::class.java).apply {
            set(value)
        }

    private fun createListProperty(value: List<String>?) =
        project.objects.listProperty(String::class.java).apply {
            set(value)
        }
}