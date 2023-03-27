package org.utbot.maven.plugin.extension

import org.apache.maven.plugin.testing.AbstractMojoTestCase
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.*
import org.utbot.common.PathUtil.toPath
import org.utbot.engine.Mocker
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.maven.plugin.GenerateTestsAndSarifReportMojo
import org.utbot.maven.plugin.TestMavenProject
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SarifMavenConfigurationProviderTest : AbstractMojoTestCase() {

    @BeforeAll
    override fun setUp() {
        super.setUp()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `targetClasses should be provided from the configuration`() {
        Assertions.assertEquals(listOf("Main"), configurationProvider.targetClasses)
    }

    @Test
    fun `projectRoot should be provided from the configuration`() {
        Assertions.assertEquals(File("build/resources/project-to-test"), configurationProvider.projectRoot)
    }

    @Test
    fun `generatedTestsRelativeRoot should be provided from the configuration`() {
        Assertions.assertEquals("target/generated/test", configurationProvider.generatedTestsRelativeRoot)
    }

    @Test
    fun `sarifReportsRelativeRoot should be provided from the configuration`() {
        Assertions.assertEquals("target/generated/sarif", configurationProvider.sarifReportsRelativeRoot)
    }

    @Test
    fun `markGeneratedTestsDirectoryAsTestSourcesRoot should be provided from the configuration`() {
        Assertions.assertEquals(true, configurationProvider.markGeneratedTestsDirectoryAsTestSourcesRoot)
    }

    @Test
    fun `testPrivateMethods should be provided from the configuration`() {
        Assertions.assertEquals(true, configurationProvider.testPrivateMethods)
    }

    @Test
    fun `projectType should be provided from the configuration`() {
        Assertions.assertEquals(ProjectType.PureJvm, configurationProvider.projectType)
    }

    @Test
    fun `testFramework should be provided from the configuration`() {
        Assertions.assertEquals(Junit5, configurationProvider.testFramework)
    }

    @Test
    fun `mockFramework should be provided from the configuration`() {
        Assertions.assertEquals(MockFramework.MOCKITO, configurationProvider.mockFramework)
    }

    @Test
    fun `generationTimeout should be provided from the configuration`() {
        Assertions.assertEquals(10000, configurationProvider.generationTimeout)
    }

    @Test
    fun `codegenLanguage should be provided from the configuration`() {
        Assertions.assertEquals(CodegenLanguage.JAVA, configurationProvider.codegenLanguage)
    }

    @Test
    fun `mockStrategy should be provided from the configuration`() {
        Assertions.assertEquals(MockStrategyApi.OTHER_PACKAGES, configurationProvider.mockStrategy)
    }

    @Test
    fun `staticsMocking should be provided from the configuration`() {
        Assertions.assertEquals(MockitoStaticMocking, configurationProvider.staticsMocking)
    }

    @Test
    fun `forceStaticMocking should be provided from the configuration`() {
        Assertions.assertEquals(ForceStaticMocking.FORCE, configurationProvider.forceStaticMocking)
    }

    @Test
    fun `classesToMockAlways should be provided from the configuration`() {
        val expectedClassesToMockAlways =
            (Mocker.defaultSuperClassesToMockAlwaysNames + "java.io.File").map(::ClassId).toSet()
        Assertions.assertEquals(expectedClassesToMockAlways, configurationProvider.classesToMockAlways)
    }

    // internal

    private val testMavenProject: TestMavenProject =
        TestMavenProject("src/test/resources/project-to-test".toPath())

    private val sarifReportMojo by lazy {
        configureSarifReportMojo(testMavenProject.mavenProject)
    }

    private val configurationProvider by lazy {
        sarifReportMojo.sarifProperties
    }

    private fun configureSarifReportMojo(mavenProject: MavenProject): GenerateTestsAndSarifReportMojo {
        val generateTestsAndSarifReportMojo = configureMojo(
            GenerateTestsAndSarifReportMojo(), "utbot-maven", mavenProject.file
        ) as GenerateTestsAndSarifReportMojo
        generateTestsAndSarifReportMojo.mavenProject = mavenProject
        return generateTestsAndSarifReportMojo
    }
}