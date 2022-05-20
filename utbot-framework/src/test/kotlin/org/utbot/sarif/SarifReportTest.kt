package org.utbot.sarif

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtTestCase
import org.junit.Test
import org.mockito.Mockito

class SarifReportTest {

    @Test
    fun testNonEmptyReport() {
        val actualReport = SarifReport(
            testCases = listOf(),
            generatedTestsCode = "",
            SourceFindingStrategyDefault("", "", "", "")
        ).createReport()

        assert(actualReport.isNotEmpty())
    }

    @Test
    fun testNoUncheckedExceptions() {
        val sarif = SarifReport(
            testCases = listOf(testCase),
            generatedTestsCode = "",
            SourceFindingStrategyDefault("", "", "", "")
        ).createReport().toSarif()

        assert(sarif.runs.first().results.isEmpty())
    }

    @Test
    fun testDetectAllUncheckedExceptions() {
        mockUtMethodNames()

        val mockUtExecutionNPE = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockUtExecutionNPE.result).thenReturn(
            UtImplicitlyThrownException(NullPointerException(), false),
        )
        Mockito.`when`(mockUtExecutionNPE.stateBefore.parameters).thenReturn(listOf())

        val mockUtExecutionAIOBE = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockUtExecutionAIOBE.result).thenReturn(
            UtImplicitlyThrownException(ArrayIndexOutOfBoundsException(), false),
        )
        Mockito.`when`(mockUtExecutionAIOBE.stateBefore.parameters).thenReturn(listOf())

        val testCases = listOf(
            UtTestCase(mockUtMethod, listOf(mockUtExecutionNPE)),
            UtTestCase(mockUtMethod, listOf(mockUtExecutionAIOBE))
        )

        val report = SarifReport(
            testCases = testCases,
            generatedTestsCode = "",
            SourceFindingStrategyDefault("", "", "", "")
        ).createReport().toSarif()

        assert(report.runs.first().results[0].message.text.contains("NullPointerException"))
        assert(report.runs.first().results[1].message.text.contains("ArrayIndexOutOfBoundsException"))
    }

    @Test
    fun testCorrectResultLocations() {
        mockUtMethodNames()

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(NullPointerException(), false)
        )
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(listOf())
        Mockito.`when`(mockUtExecution.path.lastOrNull()?.stmt?.javaSourceStartLineNumber).thenReturn(1337)
        Mockito.`when`(mockUtExecution.testMethodName).thenReturn("testMain")

        val report = SarifReport(
            testCases = listOf(testCase),
            generatedTestsCode = """
                // comment for `startLine == 2` in related location
                public void testMain() throws Throwable {
                    Main.main();
                } 
            """.trimIndent(),
            SourceFindingStrategyDefault(
                sourceClassFqn = "Main",
                sourceFilePath = "src/Main.java",
                testsFilePath = "test/MainTest.java",
                projectRootPath = "."
            )
        ).createReport().toSarif()

        val result = report.runs.first().results.first()
        val location = result.locations.first().physicalLocation
        val relatedLocation = result.relatedLocations.first().physicalLocation

        assert(location.artifactLocation.uri.contains("Main.java"))
        assert(location.region.startLine == 1337)
        assert(relatedLocation.artifactLocation.uri.contains("MainTest.java"))
        assert(relatedLocation.region.startLine == 2)
    }

    @Test
    fun testCorrectMethodParameters() {
        mockUtMethodNames()

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(NullPointerException(), false)
        )
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(
            listOf(
                UtPrimitiveModel(227),
                UtPrimitiveModel(3.14),
                UtPrimitiveModel(false)
            )
        )

        val report = SarifReport(
            testCases = listOf(testCase),
            generatedTestsCode = "",
            SourceFindingStrategyDefault(
                sourceClassFqn = "Main",
                sourceFilePath = "src/Main.java",
                testsFilePath = "test/MainTest.java",
                projectRootPath = "."
            )
        ).createReport().toSarif()

        val result = report.runs.first().results.first()
        assert(result.message.text.contains("227"))
        assert(result.message.text.contains("3.14"))
        assert(result.message.text.contains("false"))
    }

    @Test
    fun testCorrectCodeFlow() {
        mockUtMethodNames()

        val uncheckedException = Mockito.mock(NullPointerException::class.java)
        Mockito.`when`(uncheckedException.stackTrace).thenReturn(
            Array(2) {
                StackTraceElement("Main", "main", "Main.java", 17)
            }
        )

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(uncheckedException, false)
        )
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(listOf())

        val report = SarifReport(
            testCases = listOf(testCase),
            generatedTestsCode = "",
            SourceFindingStrategyDefault(
                sourceClassFqn = "Main",
                sourceFilePath = "src/Main.java",
                testsFilePath = "test/MainTest.java",
                projectRootPath = "."
            )
        ).createReport().toSarif()

        val result = report.runs.first().results.first().codeFlows.first().threadFlows.first().locations.map {
            it.location.physicalLocation
        }
        assert(result[0].artifactLocation.uri.contains("Main.java"))
        assert(result[0].region.startLine == 17)
        assert(result[1].artifactLocation.uri.contains("Main.java"))
        assert(result[1].region.startLine == 17)
    }

    // internal

    private val mockUtMethod = Mockito.mock(UtMethod::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val mockUtExecution = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val testCase = UtTestCase(mockUtMethod, listOf(mockUtExecution))

    private fun mockUtMethodNames() {
        Mockito.`when`(mockUtMethod.callable.name).thenReturn("main")
        Mockito.`when`(mockUtMethod.clazz.qualifiedName).thenReturn("Main")
    }

    private fun String.toSarif(): Sarif = jacksonObjectMapper().readValue(this)
}