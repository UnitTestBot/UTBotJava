package org.utbot.sarif

import org.junit.Test
import org.mockito.Mockito
import org.utbot.framework.plugin.api.*

class SarifReportTest {

    @Test
    fun testNonEmptyReport() {
        val actualReport = SarifReport(
            testSets = listOf(),
            generatedTestsCode = "",
            sourceFindingEmpty
        ).createReport().toJson()

        assert(actualReport.isNotEmpty())
    }

    @Test
    fun testNoUncheckedExceptions() {
        val sarif = SarifReport(
            testSets = listOf(testSet),
            generatedTestsCode = "",
            sourceFindingEmpty
        ).createReport()

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

        defaultMockCoverage(mockUtExecutionNPE)
        defaultMockCoverage(mockUtExecutionAIOBE)

        val testSets = listOf(
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecutionNPE)),
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecutionAIOBE))
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingEmpty
        ).createReport()

        assert(report.runs.first().results[0].message.text.contains("NullPointerException"))
        assert(report.runs.first().results[1].message.text.contains("ArrayIndexOutOfBoundsException"))
    }

    @Test
    fun testCorrectResultLocations() {
        mockUtMethodNames()

        Mockito.`when`(mockUtExecution.result).thenReturn(
            UtImplicitlyThrownException(NullPointerException(), false)
        )
        mockCoverage(mockUtExecution, 1337, "Main")
        Mockito.`when`(mockUtExecution.stateBefore.parameters).thenReturn(listOf())
        Mockito.`when`(mockUtExecution.testMethodName).thenReturn("testMain_ThrowArithmeticException")

        val report = sarifReportMain.createReport()

        val result = report.runs.first().results.first()
        val location = result.locations.filterIsInstance<SarifPhysicalLocationWrapper>().first().physicalLocation
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

        defaultMockCoverage(mockUtExecution)

        val report = sarifReportMain.createReport()

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

        defaultMockCoverage(mockUtExecution)

        val report = sarifReportMain.createReport()

        val result = report.runs.first().results.first().codeFlows.first().threadFlows.first().locations.map {
            it.location.physicalLocation
        }
        for (index in 0..1) {
            assert(result[index].artifactLocation.uri.contains("Main.java"))
            assert(result[index].region.startLine == 17)
        }
    }

    @Test
    fun testProcessSandboxFailure() {
        mockUtMethodNames()

        val uncheckedException = Mockito.mock(java.security.NoSuchAlgorithmException::class.java)
        Mockito.`when`(uncheckedException.stackTrace).thenReturn(arrayOf())
        Mockito.`when`(mockUtExecution.result).thenReturn(UtSandboxFailure(uncheckedException))

        defaultMockCoverage(mockUtExecution)

        val report = sarifReportMain.createReport()
        val result = report.runs.first().results.first()
        assert(result.message.text.contains("NoSuchAlgorithmException"))
    }

    @Test
    fun testProcessTimeoutException() {
        mockUtMethodNames()

        val timeoutMessage = "Timeout 1000 elapsed"
        val timeoutException = TimeoutException(timeoutMessage)
        Mockito.`when`(mockUtExecution.result).thenReturn(UtTimeoutException(timeoutException))

        defaultMockCoverage(mockUtExecution)

        val report = sarifReportMain.createReport()
        val result = report.runs.first().results.first()
        assert(result.message.text.contains(timeoutMessage))
    }

    @Test
    fun testConvertCoverageToStackTrace() {
        mockUtMethodNames()

        val timeoutException = TimeoutException("Timeout 1000 elapsed")
        Mockito.`when`(mockUtExecution.result).thenReturn(UtTimeoutException(timeoutException))

        val classMainPath = "com/abc/Main"
        val classUtilPath = "com/cba/Util"
        Mockito.`when`(mockUtExecution.symbolicSteps).thenReturn(listOf())
        Mockito.`when`(mockUtExecution.coverage?.coveredInstructions).thenReturn(listOf(
            Instruction(classMainPath, "main(l)l", 3, 1),
            Instruction(classMainPath, "main(l)l", 4, 2),
            Instruction(classMainPath, "main(l)l", 4, 3), // last for main
            Instruction(classUtilPath, "util(ll)l", 6, 4),
            Instruction(classUtilPath, "util(ll)l", 7, 5), // last for util
        ))

        val report = sarifReportMain.createReport()
        val result = report.runs.first().results.first()
        val stackTrace = result.codeFlows.first().threadFlows.first().locations

        assert(stackTrace[0].location.physicalLocation.artifactLocation.uri == "$classMainPath.java")
        assert(stackTrace[0].location.physicalLocation.region.startLine == 4)

        assert(stackTrace[1].location.physicalLocation.artifactLocation.uri == "$classUtilPath.java")
        assert(stackTrace[1].location.physicalLocation.region.startLine == 7)
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

        defaultMockCoverage(mockUtExecution)

        val report = sarifReportMain.createReport()

        val codeFlowPhysicalLocations = report.runs[0].results[0].codeFlows[0].threadFlows[0].locations.map {
            it.location.physicalLocation
        }
        assert(codeFlowPhysicalLocations[0].artifactLocation.uri.contains("MainTest.java"))
        assert(codeFlowPhysicalLocations[0].region.startLine == 5)
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

        defaultMockCoverage(mockUtExecution)

        val report = sarifReportPrivateMain.createReport()

        val codeFlowPhysicalLocations = report.runs[0].results[0].codeFlows[0].threadFlows[0].locations.map {
            it.location.physicalLocation
        }
        assert(codeFlowPhysicalLocations[0].artifactLocation.uri.contains("MainTest.java"))
        assert(codeFlowPhysicalLocations[0].region.startLine == 6)
        assert(codeFlowPhysicalLocations[0].region.startColumn == 5)
    }

    @Test
    fun testMinimizationRemovesDuplicates() {
        mockUtMethodNames()

        val mockUtExecution = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockUtExecution.result).thenReturn(UtImplicitlyThrownException(NullPointerException(), false))

        defaultMockCoverage(mockUtExecution)

        val testSets = listOf(
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution)),
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution)) // duplicate
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport()

        assert(report.runs.first().results.size == 1) // no duplicates
    }

    @Test
    fun testMinimizationDoesNotRemoveResultsWithDifferentRuleId() {
        mockUtMethodNames()

        val mockUtExecution1 = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        val mockUtExecution2 = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

        // different ruleId's
        Mockito.`when`(mockUtExecution1.result).thenReturn(UtImplicitlyThrownException(NullPointerException(), false))
        Mockito.`when`(mockUtExecution2.result).thenReturn(UtImplicitlyThrownException(ArithmeticException(), false))

        defaultMockCoverage(mockUtExecution1)
        defaultMockCoverage(mockUtExecution2)

        val testSets = listOf(
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution1)),
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution2)) // not a duplicate
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport()

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
        mockCoverage(mockUtExecution1, 11, "Main")
        mockCoverage(mockUtExecution2, 22, "Main")

        val testSets = listOf(
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution1)),
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution2)) // not a duplicate
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport()

        assert(report.runs.first().results.size == 2) // no results have been removed
    }

    @Test
    fun testMinimizationChoosesShortestCodeFlow() {
        mockUtMethodNames()

        val mockNPE1 = Mockito.mock(NullPointerException::class.java)
        val mockNPE2 = Mockito.mock(NullPointerException::class.java)

        val mockUtExecution1 = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)
        val mockUtExecution2 = Mockito.mock(UtExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

        // the same ruleId's
        Mockito.`when`(mockUtExecution1.result).thenReturn(UtImplicitlyThrownException(mockNPE1, false))
        Mockito.`when`(mockUtExecution2.result).thenReturn(UtImplicitlyThrownException(mockNPE2, false))

        // but different stack traces
        val stackTraceElement1 = StackTraceElement("Main", "main", "Main.java", 3)
        val stackTraceElement2 = StackTraceElement("Main", "main", "Main.java", 7)
        Mockito.`when`(mockNPE1.stackTrace).thenReturn(arrayOf(stackTraceElement1))
        Mockito.`when`(mockNPE2.stackTrace).thenReturn(arrayOf(stackTraceElement1, stackTraceElement2))

        defaultMockCoverage(mockUtExecution1)
        defaultMockCoverage(mockUtExecution2)

        val testSets = listOf(
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution1)),
            UtMethodTestSet(mockExecutableId, listOf(mockUtExecution2)) // duplicate with a longer stack trace
        )

        val report = SarifReport(
            testSets = testSets,
            generatedTestsCode = "",
            sourceFindingMain
        ).createReport()

        assert(report.runs.first().results.size == 1) // no duplicates
        assert(report.runs.first().results.first().totalCodeFlowLocations() == 1) // with a shorter stack trace
    }

    // internal

    private val mockExecutableId = Mockito.mock(ExecutableId::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val mockUtExecution = Mockito.mock(UtSymbolicExecution::class.java, Mockito.RETURNS_DEEP_STUBS)

    private val testSet = UtMethodTestSet(mockExecutableId, listOf(mockUtExecution))

    private fun mockUtMethodNames() {
        Mockito.`when`(mockExecutableId.name).thenReturn("main")
        Mockito.`when`(mockExecutableId.classId.name).thenReturn("Main")
    }

    private fun mockCoverage(mockExecution: UtExecution, lineNumber: Int, className: String) {
        Mockito.`when`(mockExecution.coverage?.coveredInstructions?.lastOrNull()?.lineNumber).thenReturn(1)
        Mockito.`when`(mockExecution.coverage?.coveredInstructions?.lastOrNull()?.internalName).thenReturn("Main")
        Mockito.`when`(mockExecution.coverage?.coveredInstructions?.lastOrNull()?.className).thenReturn("Main")
        (mockExecution as? UtSymbolicExecution)?.let { mockSymbolicSteps(it, lineNumber, className) }
    }

    private fun mockSymbolicSteps(mockExecution: UtSymbolicExecution, lineNumber: Int, className: String) {
        Mockito.`when`(mockExecution.symbolicSteps.lastOrNull()?.lineNumber).thenReturn(lineNumber)
        Mockito.`when`(mockExecution.symbolicSteps.lastOrNull()?.method?.declaringClass?.name).thenReturn(className)
    }

    private fun defaultMockCoverage(mockExecution: UtExecution) {
        mockCoverage(mockExecution, 1, "Main")
    }

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
            /* This test fails because method [Main.main] produces [java.lang.ArithmeticException: / by zero]
                Main.main(Main.java:15) */
            Main main = new Main();
              main.main(0); // shift for `startColumn` == 7
        }
    """.trimIndent()

    private val generatedTestsCodePrivateMain = """
        public void testMain_ThrowArithmeticException() {
            /* This test fails because method [Main.main] produces [java.lang.ArithmeticException: / by zero]
                Main.main(Main.java:15) */
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