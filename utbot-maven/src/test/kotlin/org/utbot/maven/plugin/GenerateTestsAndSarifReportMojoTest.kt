package org.utbot.maven.plugin

import org.apache.maven.plugin.testing.AbstractMojoTestCase
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.*
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.util.GeneratedSarif
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerateTestsAndSarifReportMojoTest : AbstractMojoTestCase() {

    @BeforeAll
    override fun setUp() {
        super.setUp()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `test directory exists and not empty`() {
        val testsRelativePath = sarifReportMojo.sarifProperties.generatedTestsRelativeRoot
        val testDirectory = testMavenProject.projectBaseDir.resolve(testsRelativePath)
        assert(directoryExistsAndNotEmpty(testDirectory))
    }

    @Test
    fun `sarif directory exists and not empty`() {
        val reportsRelativePath = sarifReportMojo.sarifProperties.sarifReportsRelativeRoot
        val sarifDirectory = testMavenProject.projectBaseDir.resolve(reportsRelativePath)
        assert(directoryExistsAndNotEmpty(sarifDirectory))
    }

    @Test
    fun `sarif report contains all required results`() {
        val sarifReportFile = sarifReportMojo.rootMavenProjectWrapper.sarifReportFile
        val sarifReportText = sarifReportFile.readText()
        GeneratedSarif(sarifReportText).apply {
            assert(hasSchema())
            assert(hasVersion())
            assert(hasRules())
            assert(hasResults())
            assert(hasCodeFlows())
            assert(codeFlowsIsNotEmpty())
            assert(contains("ArithmeticException"))
        }
    }

    // internal

    private val testMavenProject: TestMavenProject =
        TestMavenProject("src/test/resources/project-to-test".toPath())

    private val sarifReportMojo by lazy {
        configureSarifReportMojo(testMavenProject.mavenProject).apply {
            this.execute()
        }
    }

    private fun configureSarifReportMojo(mavenProject: MavenProject): GenerateTestsAndSarifReportMojo {
        val generateTestsAndSarifReportMojo = configureMojo(
            GenerateTestsAndSarifReportMojo(), "utbot-maven", mavenProject.file
        ) as GenerateTestsAndSarifReportMojo
        generateTestsAndSarifReportMojo.mavenProject = mavenProject
        return generateTestsAndSarifReportMojo
    }

    private fun directoryExistsAndNotEmpty(directory: File): Boolean =
        directory.exists() && directory.isDirectory && directory.list()?.isNotEmpty() == true
}