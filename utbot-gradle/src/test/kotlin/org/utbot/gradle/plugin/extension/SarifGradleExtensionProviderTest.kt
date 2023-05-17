package org.utbot.gradle.plugin.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.utbot.common.PathUtil.toPath
import org.utbot.engine.Mocker
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.NoStaticMocking
import org.utbot.framework.codegen.domain.ProjectType.*
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.TestNg
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
            setTargetClassesInExtension(null)
            assertEquals(listOf<String>(), extensionProvider.targetClasses)
        }

        @Test
        fun `should be provided from the extension`() {
            val targetClasses = listOf("com.abc.Main")
            setTargetClassesInExtension(targetClasses)
            assertEquals(targetClasses, extensionProvider.targetClasses)
        }

        @Test
        fun `should be provided from the task parameters`() {
            val targetClasses = listOf("com.abc.Main")
            setTargetClassesInTaskParameters(targetClasses)
            assertEquals(targetClasses, extensionProvider.targetClasses)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            val targetClasses = listOf("com.abc.Main")
            val anotherTargetClasses = listOf("com.abc.Another")
            setTargetClassesInTaskParameters(targetClasses)
            setTargetClassesInExtension(anotherTargetClasses)
            assertEquals(targetClasses, extensionProvider.targetClasses)
        }

        @Test
        fun `should be resolved from the keyword 'all'`() {
            extensionProvider.taskParameters = mapOf("targetClasses" to "all")
            assertEquals(listOf<String>(), extensionProvider.targetClasses)
        }

        private fun setTargetClassesInExtension(value: List<String>?) {
            Mockito.`when`(extensionMock.targetClasses).thenReturn(createListProperty(value))
        }

        private fun setTargetClassesInTaskParameters(value: List<String>) {
            extensionProvider.taskParameters = mapOf("targetClasses" to value.joinToString(",", "[", "]"))
        }
    }

    @Nested
    @DisplayName("projectRoot")
    inner class ProjectRootTest {
        @Test
        fun `should be projectDir by default`() {
            setProjectRootInExtension(null)
            assertEquals(project.projectDir, extensionProvider.projectRoot)
        }

        @Test
        fun `should be provided from the extension`() {
            val projectRoot = "some/dir/"
            setProjectRootInExtension(projectRoot)
            assertEquals(File(projectRoot), extensionProvider.projectRoot)
        }

        @Test
        fun `should be provided from the task parameters`() {
            val projectRoot = "some/directory/"
            setProjectRootInTaskParameters(projectRoot)
            assertEquals(File(projectRoot), extensionProvider.projectRoot)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            val projectRoot = "some/dir/"
            val anotherProjectRoot = "another/dir/"
            setProjectRootInTaskParameters(projectRoot)
            setProjectRootInExtension(anotherProjectRoot)
            assertEquals(File(projectRoot), extensionProvider.projectRoot)
        }

        private fun setProjectRootInExtension(value: String?) {
            Mockito.`when`(extensionMock.projectRoot).thenReturn(createStringProperty(value))
        }

        private fun setProjectRootInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("projectRoot" to value)
        }
    }

    @Nested
    @DisplayName("generatedTestsRelativeRoot")
    inner class GeneratedTestsRelativeRootTest {
        @Test
        fun `should be build generated test by default`() {
            val testsRootExpected = "build/generated/test"
            setGeneratedTestsRelativeRootInExtension(null)
            assertEquals(testsRootExpected.toPath(), extensionProvider.generatedTestsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the extension`() {
            val testsRoot = "some/dir/"
            setGeneratedTestsRelativeRootInExtension(testsRoot)
            assertEquals(testsRoot.toPath(), extensionProvider.generatedTestsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the task parameters`() {
            val testsRoot = "some/directory/"
            setGeneratedTestsRelativeRootInTaskParameters(testsRoot)
            assertEquals(testsRoot.toPath(), extensionProvider.generatedTestsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            val testsRoot = "some/dir/"
            val anotherTestsRoot = "another/dir/"
            setGeneratedTestsRelativeRootInTaskParameters(testsRoot)
            setGeneratedTestsRelativeRootInExtension(anotherTestsRoot)
            assertEquals(testsRoot.toPath(), extensionProvider.generatedTestsRelativeRoot.toPath())
        }

        private fun setGeneratedTestsRelativeRootInExtension(value: String?) {
            Mockito.`when`(extensionMock.generatedTestsRelativeRoot).thenReturn(createStringProperty(value))
        }

        private fun setGeneratedTestsRelativeRootInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("generatedTestsRelativeRoot" to value)
        }
    }

    @Nested
    @DisplayName("sarifReportsRelativeRoot")
    inner class SarifReportsRelativeRootTest {
        @Test
        fun `should be build generated sarif by default`() {
            setSarifReportsRelativeRootInExtension(null)
            val sarifRoot = "build/generated/sarif"
            assertEquals(sarifRoot.toPath(), extensionProvider.sarifReportsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the extension`() {
            val sarifRoot = "some/dir/"
            setSarifReportsRelativeRootInExtension(sarifRoot)
            assertEquals(sarifRoot.toPath(), extensionProvider.sarifReportsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the task parameters`() {
            val sarifRoot = "some/directory/"
            setSarifReportsRelativeRootInTaskParameters(sarifRoot)
            assertEquals(sarifRoot.toPath(), extensionProvider.sarifReportsRelativeRoot.toPath())
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            val sarifRoot = "some/dir/"
            val anotherSarifRoot = "another/dir/"
            setSarifReportsRelativeRootInTaskParameters(sarifRoot)
            setSarifReportsRelativeRootInExtension(anotherSarifRoot)
            assertEquals(sarifRoot.toPath(), extensionProvider.sarifReportsRelativeRoot.toPath())
        }

        private fun setSarifReportsRelativeRootInExtension(value: String?) {
            Mockito.`when`(extensionMock.sarifReportsRelativeRoot).thenReturn(createStringProperty(value))
        }

        private fun setSarifReportsRelativeRootInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("sarifReportsRelativeRoot" to value)
        }
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

        private fun setMark(value: Boolean?) {
            Mockito.`when`(extensionMock.markGeneratedTestsDirectoryAsTestSourcesRoot)
                .thenReturn(createBooleanProperty(value))
        }
    }

    @Nested
    @DisplayName("testPrivateMethods")
    inner class TestPrivateMethodsTest {
        @Test
        fun `should be false by default`() {
            setTestPrivateMethodsInExtension(null)
            assertEquals(false, extensionProvider.testPrivateMethods)
        }

        @Test
        fun `should be provided from the extension`() {
            setTestPrivateMethodsInExtension(true)
            assertEquals(true, extensionProvider.testPrivateMethods)
        }

        @Test
        fun `should be provided from the task parameters`() {
            setTestPrivateMethodsInTaskParameters(true)
            assertEquals(true, extensionProvider.testPrivateMethods)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setTestPrivateMethodsInTaskParameters(false)
            setTestPrivateMethodsInExtension(true)
            assertEquals(false, extensionProvider.testPrivateMethods)
        }

        private fun setTestPrivateMethodsInExtension(value: Boolean?) {
            Mockito.`when`(extensionMock.testPrivateMethods).thenReturn(createBooleanProperty(value))
        }

        private fun setTestPrivateMethodsInTaskParameters(value: Boolean) {
            extensionProvider.taskParameters = mapOf("testPrivateMethods" to "$value")
        }
    }

    @Nested
    @DisplayName("projectType")
    inner class ProjectTypeTest {
        @Test
        fun `should be ProjectType defaultItem by default`() {
            setProjectTypeInExtension(null)
            assertEquals(PureJvm, extensionProvider.projectType)
        }

        @Test
        fun `should be equal to PureJvm`() {
            setProjectTypeInExtension("purejvm")
            assertEquals(PureJvm, extensionProvider.projectType)
        }

        @Test
        fun `should be equal to Spring`() {
            setProjectTypeInExtension("spring")
            assertEquals(Spring, extensionProvider.projectType)
        }

        @Test
        fun `should be equal to Python`() {
            setProjectTypeInExtension("python")
            assertEquals(Python, extensionProvider.projectType)
        }

        @Test
        fun `should be equal to JavaScript`() {
            setProjectTypeInExtension("javascript")
            assertEquals(JavaScript, extensionProvider.projectType)
        }

        @Test
        fun `should fail on unknown project type`() {
            setProjectTypeInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.projectType
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setProjectTypeInTaskParameters("spring")
            assertEquals(Spring, extensionProvider.projectType)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setProjectTypeInTaskParameters("python")
            setProjectTypeInExtension("javascript")
            assertEquals(Python, extensionProvider.projectType)
        }

        private fun setProjectTypeInExtension(value: String?) {
            Mockito.`when`(extensionMock.projectType).thenReturn(createStringProperty(value))
        }

        private fun setProjectTypeInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("projectType" to value)
        }
    }

    @Nested
    @DisplayName("testFramework")
    inner class TestFrameworkTest {
        @Test
        fun `should be TestFramework defaultItem by default`() {
            setTestFrameworkInExtension(null)
            assertEquals(TestFramework.defaultItem, extensionProvider.testFramework)
        }

        @Test
        fun `should be equal to Junit4`() {
            setTestFrameworkInExtension("junit4")
            assertEquals(Junit4, extensionProvider.testFramework)
        }

        @Test
        fun `should be equal to Junit5`() {
            setTestFrameworkInExtension("junit5")
            assertEquals(Junit5, extensionProvider.testFramework)
        }

        @Test
        fun `should be equal to TestNg`() {
            setTestFrameworkInExtension("testng")
            assertEquals(TestNg, extensionProvider.testFramework)
        }

        @Test
        fun `should fail on unknown test framework`() {
            setTestFrameworkInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.testFramework
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setTestFrameworkInTaskParameters("junit4")
            assertEquals(Junit4, extensionProvider.testFramework)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setTestFrameworkInTaskParameters("testng")
            setTestFrameworkInExtension("junit5")
            assertEquals(TestNg, extensionProvider.testFramework)
        }

        private fun setTestFrameworkInExtension(value: String?) {
            Mockito.`when`(extensionMock.testFramework).thenReturn(createStringProperty(value))
        }

        private fun setTestFrameworkInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("testFramework" to value)
        }
    }

    @Nested
    @DisplayName("mockFramework")
    inner class MockFrameworkTest {
        @Test
        fun `should be MockFramework defaultItem by default`() {
            setMockFrameworkInExtension(null)
            assertEquals(MockFramework.defaultItem, extensionProvider.mockFramework)
        }

        @Test
        fun `should be equal to MOCKITO`() {
            setMockFrameworkInExtension("mockito")
            assertEquals(MockFramework.MOCKITO, extensionProvider.mockFramework)
        }

        @Test
        fun `should fail on unknown mock framework`() {
            setMockFrameworkInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.mockFramework
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setMockFrameworkInTaskParameters("mockito")
            assertEquals(MockFramework.MOCKITO, extensionProvider.mockFramework)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setMockFrameworkInTaskParameters("unknown")
            setMockFrameworkInExtension("mockito")
            assertThrows<IllegalStateException> {
                extensionProvider.mockFramework
            }
        }

        private fun setMockFrameworkInExtension(value: String?) {
            Mockito.`when`(extensionMock.mockFramework).thenReturn(createStringProperty(value))
        }

        private fun setMockFrameworkInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("mockFramework" to value)
        }
    }

    @Nested
    @DisplayName("generationTimeout")
    inner class GenerationTimeoutTest {
        @Test
        fun `should be 60 seconds by default`() {
            setGenerationTimeoutInExtension(null)
            assertEquals(60 * 1000L, extensionProvider.generationTimeout)
        }

        @Test
        fun `should be provided from the extension`() {
            setGenerationTimeoutInExtension(100L)
            assertEquals(100L, extensionProvider.generationTimeout)
        }

        @Test
        fun `should be provided from the task parameters`() {
            setGenerationTimeoutInTaskParameters("100")
            assertEquals(100L, extensionProvider.generationTimeout)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setGenerationTimeoutInTaskParameters("999")
            setGenerationTimeoutInExtension(100L)
            assertEquals(999L, extensionProvider.generationTimeout)
        }

        @Test
        fun `should fail on negative timeout`() {
            setGenerationTimeoutInExtension(-1)
            assertThrows<IllegalStateException> {
                extensionProvider.generationTimeout
            }
        }

        private fun setGenerationTimeoutInExtension(value: Long?) {
            Mockito.`when`(extensionMock.generationTimeout).thenReturn(createLongProperty(value))
        }

        private fun setGenerationTimeoutInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("generationTimeout" to value)
        }
    }

    @Nested
    @DisplayName("codegenLanguage")
    inner class CodegenLanguageTest {
        @Test
        fun `should be CodegenLanguage defaultItem by default`() {
            setCodegenLanguageInExtension(null)
            assertEquals(CodegenLanguage.defaultItem, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should be equal to JAVA`() {
            setCodegenLanguageInExtension("java")
            assertEquals(CodegenLanguage.JAVA, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should be equal to KOTLIN`() {
            setCodegenLanguageInExtension("kotlin")
            assertEquals(CodegenLanguage.KOTLIN, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should fail on unknown codegen language`() {
            setCodegenLanguageInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.codegenLanguage
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setCodegenLanguageInTaskParameters("kotlin")
            assertEquals(CodegenLanguage.KOTLIN, extensionProvider.codegenLanguage)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setCodegenLanguageInTaskParameters("java")
            setCodegenLanguageInExtension("kotlin")
            assertEquals(CodegenLanguage.JAVA, extensionProvider.codegenLanguage)
        }

        private fun setCodegenLanguageInExtension(value: String?) {
            Mockito.`when`(extensionMock.codegenLanguage).thenReturn(createStringProperty(value))
        }

        private fun setCodegenLanguageInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("codegenLanguage" to value)
        }
    }

    @Nested
    @DisplayName("mockStrategy")
    inner class MockStrategyTest {
        @Test
        fun `should be MockStrategyApi defaultItem by default`() {
            setMockStrategyInExtension(null)
            assertEquals(MockStrategyApi.defaultItem, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be equal to NO_MOCKS`() {
            setMockStrategyInExtension("no-mocks")
            assertEquals(MockStrategyApi.NO_MOCKS, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be equal to OTHER_PACKAGES`() {
            setMockStrategyInExtension("other-packages")
            assertEquals(MockStrategyApi.OTHER_PACKAGES, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be equal to OTHER_CLASSES`() {
            setMockStrategyInExtension("other-classes")
            assertEquals(MockStrategyApi.OTHER_CLASSES, extensionProvider.mockStrategy)
        }

        @Test
        fun `should fail on unknown mock strategy`() {
            setMockStrategyInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.mockStrategy
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setMockStrategyInTaskParameters("no-mocks")
            assertEquals(MockStrategyApi.NO_MOCKS, extensionProvider.mockStrategy)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setMockStrategyInTaskParameters("other-packages")
            setMockStrategyInExtension("other-classes")
            assertEquals(MockStrategyApi.OTHER_PACKAGES, extensionProvider.mockStrategy)
        }

        private fun setMockStrategyInExtension(value: String?) {
            Mockito.`when`(extensionMock.mockStrategy).thenReturn(createStringProperty(value))
        }

        private fun setMockStrategyInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("mockStrategy" to value)
        }
    }

    @Nested
    @DisplayName("staticsMocking")
    inner class StaticsMockingTest {
        @Test
        fun `should be StaticsMocking defaultItem by default`() {
            setStaticsMockingInExtension(null)
            assertEquals(StaticsMocking.defaultItem, extensionProvider.staticsMocking)
        }

        @Test
        fun `should be equal to NoStaticMocking`() {
            setStaticsMockingInExtension("do-not-mock-statics")
            assertEquals(NoStaticMocking, extensionProvider.staticsMocking)
        }

        @Test
        fun `should be equal to`() {
            setStaticsMockingInExtension("mock-statics")
            assertEquals(MockitoStaticMocking, extensionProvider.staticsMocking)
        }

        @Test
        fun `should fail on unknown statics mocking`() {
            setStaticsMockingInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.staticsMocking
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setStaticsMockingInTaskParameters("do-not-mock-statics")
            assertEquals(NoStaticMocking, extensionProvider.staticsMocking)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setStaticsMockingInTaskParameters("mock-statics")
            setStaticsMockingInExtension("do-not-mock-statics")
            assertEquals(MockitoStaticMocking, extensionProvider.staticsMocking)
        }

        private fun setStaticsMockingInExtension(value: String?) {
            Mockito.`when`(extensionMock.staticsMocking).thenReturn(createStringProperty(value))
        }

        private fun setStaticsMockingInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("staticsMocking" to value)
        }
    }

    @Nested
    @DisplayName("forceStaticMocking")
    inner class ForceStaticMockingTest {
        @Test
        fun `should be ForceStaticMocking defaultItem by default`() {
            setForceStaticMockingInExtension(null)
            assertEquals(ForceStaticMocking.defaultItem, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should be equal to FORCE`() {
            setForceStaticMockingInExtension("force")
            assertEquals(ForceStaticMocking.FORCE, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should be equal to DO_NOT_FORCE`() {
            setForceStaticMockingInExtension("do-not-force")
            assertEquals(ForceStaticMocking.DO_NOT_FORCE, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should fail on unknown force static mocking`() {
            setForceStaticMockingInExtension("unknown")
            assertThrows<IllegalStateException> {
                extensionProvider.forceStaticMocking
            }
        }

        @Test
        fun `should be provided from the task parameters`() {
            setForceStaticMockingInTaskParameters("do-not-force")
            assertEquals(ForceStaticMocking.DO_NOT_FORCE, extensionProvider.forceStaticMocking)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            setForceStaticMockingInTaskParameters("force")
            setForceStaticMockingInExtension("do-not-force")
            assertEquals(ForceStaticMocking.FORCE, extensionProvider.forceStaticMocking)
        }

        private fun setForceStaticMockingInExtension(value: String?) {
            Mockito.`when`(extensionMock.forceStaticMocking).thenReturn(createStringProperty(value))
        }

        private fun setForceStaticMockingInTaskParameters(value: String) {
            extensionProvider.taskParameters = mapOf("forceStaticMocking" to value)
        }
    }

    @Nested
    @DisplayName("classesToMockAlways")
    inner class ClassesToMockAlwaysTest {

        private val defaultClasses =
            Mocker.defaultSuperClassesToMockAlwaysNames.map(::ClassId).toSet()

        @Test
        fun `should be defaultSuperClassesToMockAlwaysNames by default`() {
            setClassesToMockAlwaysInExtension(null)
            assertEquals(defaultClasses, extensionProvider.classesToMockAlways)
        }

        @Test
        fun `should be provided from the extension`() {
            val classes = listOf("com.abc.Main")
            val expectedClasses = classes.map(::ClassId).toSet() + defaultClasses
            setClassesToMockAlwaysInExtension(classes)
            assertEquals(expectedClasses, extensionProvider.classesToMockAlways)
        }

        @Test
        fun `should be provided from the task parameters`() {
            val classes = listOf("com.abc.Main")
            val expectedClasses = classes.map(::ClassId).toSet() + defaultClasses
            setClassesToMockAlwaysInTaskParameters(classes)
            assertEquals(expectedClasses, extensionProvider.classesToMockAlways)
        }

        @Test
        fun `should be provided from the task parameters, not from the extension`() {
            val classes = listOf("com.abc.Main")
            val anotherClasses = listOf("com.abc.Another")
            val expectedClasses = classes.map(::ClassId).toSet() + defaultClasses
            setClassesToMockAlwaysInTaskParameters(classes)
            setClassesToMockAlwaysInExtension(anotherClasses)
            assertEquals(expectedClasses, extensionProvider.classesToMockAlways)
        }

        private fun setClassesToMockAlwaysInExtension(value: List<String>?) =
            Mockito.`when`(extensionMock.classesToMockAlways).thenReturn(createListProperty(value))

        private fun setClassesToMockAlwaysInTaskParameters(value: List<String>) {
            extensionProvider.taskParameters = mapOf("classesToMockAlways" to value.joinToString(",", "[", "]"))
        }
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