package org.utbot.sarif

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.junit.Test
import org.mockito.Mockito

class SarifReportTest {

    @Test
    fun testNonEmptyReport() {
        val actualReport = SarifReport(
            testSets = listOf(),
            generatedTestsCode = "",
            sourceFindingEmpty
        ).createReport()

        assert(actualReport.isNotEmpty())
    }

    @Test
    fun testNoUncheckedExceptions() {
        val sarif = SarifReport(
            testSets = listOf(testSet),
            generatedTestsCode = "",
            sourceFindingEmpty
        ).createReport().toSarif()

        assert(sarif.runs.first().results.isEmpty())
    }

    @Test
    fun testDetectAllUncheckedExceptions() {
        mockUtMethodNames()

        val mockUtExecutionNPE = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockUtExecutionNPE.result).thenReturn(
            UtImplicitlyThrownException(NullPointerException(), false),
        )
        Mockito.`when`(mockUtExecutionNPE.stateBefore.parameters).thenReturn(listOf())

        val mockUtExecutionAIOBE = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockUtExecutionAIOBE.result).thenReturn(
            UtImplicitlyThrownException(ArrayIndexOutOfBoundsException(), false),
        )
        Mockito.`when`(mockUtExecutionAIOBE.stateBefore.parameters).thenReturn(listOf())

        val testSets = listOf(
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecutionNPE)),
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecutionAIOBE))
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingEmpty
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
        Mockito.`when`(mockUtExecution.testMethodName).thenReturn("testMain_ThrowArithmeticException")

        val report = sarifReportMain.createReport().toSarif()

        val result = report.runs.first().results.first()
        val location = result.locations.first().physicalLocation
        val relatedLocation = result.relatedLocations.first().physicalLocation

        assert(location.artifactLocation.uri.contains("Main.java"))
        assert(location.region.startLine == 1337)
        assert(relatedLocation.artifactLocation.uri.contains("MainTest.java"))
        assert(relatedLocation.region.startLine == 1)
        assert(relatedLocation.region.startColumn == 1)
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

        val report = sarifReportMain.createReport().toSarif()

        val result = report.runs.first().results.first()
        assert(result.message.text.contains("227"))
        assert(result.message.text.contains("3.14"))
        assert(result.message.text.contains("false"))
    }

    @Test
    fun testCorrectCodeFlows() {
        mockUtMethodNames()

        val uncheckedException = Mockito.mock(NullPointerException::class.java)
        val stackTraceElement = StackTraceElement("Main", "main", "Main.java", 17)
        Mockito.`when`(uncheckedException.stackTrace).thenReturn(
            Array(2) { stackTraceElement }
        )

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(uncheckedException, false)
        )
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(listOf())

        val report = sarifReportMain.createReport().toSarif()

        val result = report.runs.first().results.first().codeFlows.first().threadFlows.first().locations.map {
            it.location.physicalLocation
        }
        for (index in 0..1) {
            assert(result[index].artifactLocation.uri.contains("Main.java"))
            assert(result[index].region.startLine == 17)
        }
    }

    @Test
    fun testCodeFlowsStartsWithMethodCall() {
        mockUtMethodNames()

        val uncheckedException = Mockito.mock(NullPointerException::class.java)
        val stackTraceElement = StackTraceElement("Main", "main", "Main.java", 3)
        Mockito.`when`(uncheckedException.stackTrace).thenReturn(arrayOf(stackTraceElement))

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(uncheckedException, false)
        )
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(listOf())
        Mockito.`when`(mockUtExecution.testMethodName).thenReturn("testMain_ThrowArithmeticException")

        val report = sarifReportMain.createReport().toSarif()

        val codeFlowPhysicalLocations = report.runs[0].results[0].codeFlows[0].threadFlows[0].locations.map {
            it.location.physicalLocation
        }
        assert(codeFlowPhysicalLocations[0].artifactLocation.uri.contains("MainTest.java"))
        assert(codeFlowPhysicalLocations[0].region.startLine == 3)
        assert(codeFlowPhysicalLocations[0].region.startColumn == 7)
    }

    @Test
    fun testCodeFlowsStartsWithPrivateMethodCall() {
        mockUtMethodNames()

        val uncheckedException = Mockito.mock(NullPointerException::class.java)
        val stackTraceElement = StackTraceElement("Main", "main", "Main.java", 3)
        Mockito.`when`(uncheckedException.stackTrace).thenReturn(arrayOf(stackTraceElement))

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(uncheckedException, false)
        )
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(listOf())
        Mockito.`when`(mockUtExecution.testMethodName).thenReturn("testMain_ThrowArithmeticException")

        val report = sarifReportPrivateMain.createReport().toSarif()

        val codeFlowPhysicalLocations = report.runs[0].results[0].codeFlows[0].threadFlows[0].locations.map {
            it.location.physicalLocation
        }
        assert(codeFlowPhysicalLocations[0].artifactLocation.uri.contains("MainTest.java"))
        assert(codeFlowPhysicalLocations[0].region.startLine == 4)
        assert(codeFlowPhysicalLocations[0].region.startColumn == 5)
    }

    @Test
    fun testMinimizationRemovesDuplicates() {
        mockUtMethodNames()

        val mockUtExecution = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockUtExecution.result).thenReturn(UtImplicitlyThrownException(NullPointerException(), false))

        val testSets = listOf(
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution)),
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution)) // duplicate
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport().toSarif()

        assert(report.runs.first().results.size == 1) // no duplicates
    }

    @Test
    fun testMinimizationDoesNotRemoveResultsWithDifferentRuleId() {
        mockUtMethodNames()

        val mockUtExecution1 = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        val mockUtExecution2 = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

        // different ruleId's
        Mockito.`when`(mockUtExecution1.result).thenReturn(UtImplicitlyThrownException(NullPointerException(), false))
        Mockito.`when`(mockUtExecution2.result).thenReturn(UtImplicitlyThrownException(ArithmeticException(), false))

        val testSets = listOf(
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution1)),
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution2)) // not a duplicate
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport().toSarif()

        assert(report.runs.first().results.size == 2) // no results have been removed
    }

    @Test
    fun testMinimizationDoesNotRemoveResultsWithDifferentLocations() {
        mockUtMethodNames()

        val mockUtExecution1 = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        val mockUtExecution2 = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

        // the same ruleId's
        Mockito.`when`(mockUtExecution1.result).thenReturn(UtImplicitlyThrownException(NullPointerException(), false))
        Mockito.`when`(mockUtExecution2.result).thenReturn(UtImplicitlyThrownException(NullPointerException(), false))

        // different locations
        Mockito.`when`(mockUtExecution1.path.lastOrNull()?.stmt?.javaSourceStartLineNumber).thenReturn(11)
        Mockito.`when`(mockUtExecution2.path.lastOrNull()?.stmt?.javaSourceStartLineNumber).thenReturn(22)

        val testSets = listOf(
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution1)),
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution2)) // not a duplicate
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport().toSarif()

        assert(report.runs.first().results.size == 2) // no results have been removed
    }

    @Test
    fun testMinimizationChoosesShortestCodeFlow() {
        mockUtMethodNames()

        val mockNPE1 = Mockito.mock(NullPointerException::class.java)
        val mockNPE2 = Mockito.mock(NullPointerException::class.java)

        val mockUtExecution1 = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        val mockUtExecution2 = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

        // the same ruleId's
        Mockito.`when`(mockUtExecution1.result).thenReturn(UtImplicitlyThrownException(mockNPE1, false))
        Mockito.`when`(mockUtExecution2.result).thenReturn(UtImplicitlyThrownException(mockNPE2, false))

        // but different stack traces
        val stackTraceElement1 = StackTraceElement("Main", "main", "Main.java", 3)
        val stackTraceElement2 = StackTraceElement("Main", "main", "Main.java", 7)
        Mockito.`when`(mockNPE1.stackTrace).thenReturn(arrayOf(stackTraceElement1))
        Mockito.`when`(mockNPE2.stackTrace).thenReturn(arrayOf(stackTraceElement1, stackTraceElement2))

        val testSets = listOf(
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution1)),
            UtMethodTestSet(mockUtMethod, listOf(mockUtExecution2)) // duplicate with a longer stack trace
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport().toSarif()

        assert(report.runs.first().results.size == 1) // no duplicates
        assert(report.runs.first().results.first().totalCodeFlowLocations() == 1) // with a shorter stack trace
    }

    // internal

    private val mockUtMethod = Mockito.mock(UtMethod::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val mockUtExecution = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val testSet = UtMethodTestSet(mockUtMethod, listOf(mockUtExecution))

    private fun mockUtMethodNames() {
        Mockito.`when`(mockUtMethod.callable.name).thenReturn("main")
        Mockito.`when`(mockUtMethod.clazz.qualifiedName).thenReturn("Main")
    }

    private fun String.toSarif(): Sarif = jacksonObjectMapper().readValue(this)

    // constants

    private val sourceFindingEmpty = SourceFindingStrategyDefault(
        sourceClassFqn = "",
        sourceFilePath = "",
        testsFilePath = "",
        projectRootPath = ""
    )

    private val sourceFindingMain = SourceFindingStrategyDefault(
        sourceClassFqn = "Main",
        sourceFilePath = "src/Main.java",
        testsFilePath = "test/MainTest.java",
        projectRootPath = "."
    )

    private val generatedTestsCodeMain = """
        public void testMain_ThrowArithmeticException() {
            Main main = new Main();
              main.main(0); // shift for `startColumn` == 7
        }
    """.trimIndent()

    private val generatedTestsCodePrivateMain = """
        public void testMain_ThrowArithmeticException() {
            Main main = new Main();
            // ...
            mainMethod.invoke(main, mainMethodArguments);
        }
    """.trimIndent()

    private val sarifReportMain =
        SarifReport(listOf(testSet), generatedTestsCodeMain, sourceFindingMain)

    private val sarifReportPrivateMain =
        SarifReport(listOf(testSet), generatedTestsCodePrivateMain, sourceFindingMain)
}