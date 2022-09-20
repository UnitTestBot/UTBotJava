package org.utbot.sarif

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.utbot.common.PathUtil.fileExtension
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtSymbolicExecution

/**
 * Used for the SARIF report creation by given test cases and generated tests code.
 * SARIF is a JSON-based format for presenting static analyzer results.
 * You can open a report (*.sarif file) with the VS Code extension "Sarif Viewer".
 *
 * [Read more about SARIF rules](https://github.com/microsoft/sarif-tutorials/blob/main/README.md)
 * [Sample report](https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning#example-with-minimum-required-properties)
 */
class SarifReport(
    private val testSets: List<UtMethodTestSet>,
    private val generatedTestsCode: String,
    private val sourceFinding: SourceFindingStrategy
) {

    companion object {

        /**
         * Merges several SARIF reports given as JSON-strings into one
         */
        fun mergeReports(reports: List<String>): String =
            reports.fold(Sarif.empty()) { sarif: Sarif, report: String ->
                sarif.copy(runs = sarif.runs + report.jsonToSarif().runs)
            }.sarifToJson()

        // internal

        private fun String.jsonToSarif(): Sarif =
            jacksonObjectMapper().readValue(this)

        private fun Sarif.sarifToJson(): String =
            jacksonObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(this)
    }

    /**
     * Creates a SARIF report and returns it as string
     */
    fun createReport(): String =
        constructSarif().sarifToJson()

    // internal

    private val defaultLineNumber = 1 // if the line in the source code where the exception is thrown is unknown

    /**
     * [Read more about links to locations](https://github.com/microsoft/sarif-tutorials/blob/main/docs/3-Beyond-basics.md#msg-links-location)
     */
    private val relatedLocationId = 1 // for attaching link to generated test in related locations

    private fun constructSarif(): Sarif {
        val sarifResults = mutableListOf<SarifResult>()
        val sarifRules = mutableSetOf<SarifRule>()

        for (testSet in testSets) {
            for (execution in testSet.executions) {
                if (shouldProcessExecutionResult(execution.result)) {
                    val (sarifResult, sarifRule) = processUncheckedException(
                        method = testSet.method,
                        utExecution = execution,
                        uncheckedException = execution.result as UtExecutionFailure
                    )
                    sarifResults += sarifResult
                    sarifRules += sarifRule
                }
            }
        }

        return Sarif.fromRun(
            SarifRun(
                SarifTool.fromRules(sarifRules.toList()),
                minimizeResults(sarifResults)
            )
        )
    }

    /**
     * Minimizes detected errors and removes duplicates.
     *
     * Between two [SarifResult]s with the same `ruleId` and `locations`
     * it chooses the one with the shorter length of the execution trace.
     *
     * __Example:__
     *
     * The SARIF report for the code below contains only one unchecked exception in `methodB`.
     * But without minimization, the report will contain two results: for `methodA` and for `methodB`.
     *
     * ```
     * class Example {
     *     int methodA(int a) {
     *         return methodB(a);
     *     }
     *     int methodB(int b) {
     *         return 1 / b;
     *     }
     * }
     * ```
     */
    private fun minimizeResults(sarifResults: List<SarifResult>): List<SarifResult> {
        val groupedResults = sarifResults.groupBy { sarifResult ->
            Pair(sarifResult.ruleId, sarifResult.locations)
        }
        val minimizedResults = groupedResults.map { (_, sarifResultsGroup) ->
            sarifResultsGroup.minByOrNull { sarifResult ->
                sarifResult.totalCodeFlowLocations()
            }!!
        }
        return minimizedResults
    }

    private fun processUncheckedException(
        method: ExecutableId,
        utExecution: UtExecution,
        uncheckedException: UtExecutionFailure
    ): Pair<SarifResult, SarifRule> {

        val exceptionName = uncheckedException.exception::class.java.simpleName
        val ruleId = "utbot.unchecked.$exceptionName"

        val methodName = method.name
        val classFqn = method.classId.name
        val methodArguments = utExecution.stateBefore.parameters
            .joinToString(prefix = "", separator = ", ", postfix = "") { it.preview() }

        val sarifResult = SarifResult(
            ruleId,
            Level.Error,
            Message(
                text = """
                    Unchecked exception: ${uncheckedException.exception}.
                    Test case: `$methodName($methodArguments)`
                    [Generated test for this case]($relatedLocationId)
                """.trimIndent()
            ),
            getLocations(utExecution, classFqn),
            getRelatedLocations(utExecution),
            getCodeFlows(method, utExecution, uncheckedException)
        )
        val sarifRule = SarifRule(
            ruleId,
            exceptionName,
            SarifRule.Description(
                text = "Unchecked $exceptionName detected."
            ),
            SarifRule.Description(
                text = "Seems like an exception $exceptionName might be thrown."
            ),
            SarifRule.Help(
                text = "https://docs.oracle.com/javase/8/docs/api/java/lang/$exceptionName.html"
            )
        )

        return Pair(sarifResult, sarifRule)
    }

    private fun getLocations(utExecution: UtExecution, classFqn: String?): List<SarifPhysicalLocationWrapper> {
        if (classFqn == null)
            return listOf()
        val sourceRelativePath = sourceFinding.getSourceRelativePath(classFqn)
        val startLine = getLastLineNumber(utExecution) ?: defaultLineNumber
        val sourceCode = sourceFinding.getSourceFile(classFqn)?.readText() ?: ""
        val sourceRegion = SarifRegion.withStartLine(sourceCode, startLine)
        return listOf(
            SarifPhysicalLocationWrapper(
                SarifPhysicalLocation(SarifArtifact(sourceRelativePath), sourceRegion)
            )
        )
    }

    private fun getRelatedLocations(utExecution: UtExecution): List<SarifRelatedPhysicalLocationWrapper> {
        val startLine = utExecution.testMethodName?.let { testMethodName ->
            val neededLine = generatedTestsCode.split('\n').indexOfFirst { line ->
                line.contains("$testMethodName(")
            }
            if (neededLine == -1) null else neededLine + 1 // to one-based
        } ?: defaultLineNumber
        val sourceRegion = SarifRegion.withStartLine(generatedTestsCode, startLine)
        return listOf(
            SarifRelatedPhysicalLocationWrapper(
                relatedLocationId,
                SarifPhysicalLocation(SarifArtifact(sourceFinding.testsRelativePath), sourceRegion)
            )
        )
    }

    private fun getCodeFlows(
        method: ExecutableId,
        utExecution: UtExecution,
        uncheckedException: UtExecutionFailure
    ): List<SarifCodeFlow> {
        /* Example of a typical stack trace:
            - java.lang.Math.multiplyExact(Math.java:867)
            - com.abc.Util.multiply(Util.java:10)
            - com.abc.Util.multiply(Util.java:6)
            - com.abc.Main.example(Main.java:11) // <- `lastMethodCallIndex`
            - sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            - ...
         */
        val stackTrace = uncheckedException.exception.stackTrace

        val lastMethodCallIndex = stackTrace.indexOfLast {
            it.className == method.classId.name && it.methodName == method.name
        }
        if (lastMethodCallIndex == -1)
            return listOf()
        // taking all elements before the last `method` call
        val stackTraceFiltered = stackTrace.take(lastMethodCallIndex + 1)

        val stackTraceResolved = stackTraceFiltered.mapNotNull {
            findStackTraceElementLocation(it)
        }.toMutableList()
        if (stackTraceResolved.isEmpty())
            return listOf() // empty stack trace is not shown

        // prepending stack trace by `method` call in generated tests
        val methodCallLocation: SarifPhysicalLocation? =
            findMethodCallInTestBody(utExecution.testMethodName, method.name)
        if (methodCallLocation != null) {
            val methodCallLocationWrapper = SarifFlowLocationWrapper(
                SarifFlowLocation(
                    message = Message(
                        text = "${sourceFinding.testsRelativePath.toPath().fileName}:${methodCallLocation.region.startLine}"
                    ),
                    physicalLocation = methodCallLocation
                )
            )
            stackTraceResolved.add(methodCallLocationWrapper) // will be first after reverse
        }

        return listOf(
            SarifCodeFlow.fromLocations(
                stackTraceResolved.reversed() // reversing stackTrace for convenience
            )
        )
    }

    private fun findStackTraceElementLocation(stackTraceElement: StackTraceElement): SarifFlowLocationWrapper? {
        val lineNumber = stackTraceElement.lineNumber
        if (lineNumber < 1)
            return null
        val extension = stackTraceElement.fileName?.toPath()?.fileExtension
        val relativePath = sourceFinding.getSourceRelativePath(stackTraceElement.className, extension)
        val sourceCode = sourceFinding.getSourceFile(stackTraceElement.className, extension)?.readText() ?: ""
        return SarifFlowLocationWrapper(
            SarifFlowLocation(
                message = Message(
                    text = stackTraceElement.toString()
                ),
                physicalLocation = SarifPhysicalLocation(
                    SarifArtifact(relativePath),
                    SarifRegion.withStartLine(sourceCode, lineNumber)
                )
            )
        )
    }

    private val testsBodyLines by lazy {
        generatedTestsCode.split('\n')
    }

    private fun findMethodCallInTestBody(testMethodName: String?, methodName: String): SarifPhysicalLocation? {
        if (testMethodName == null)
            return null

        // searching needed test
        val testMethodStartsAt = testsBodyLines.indexOfFirst { line ->
            line.contains(testMethodName)
        }
        if (testMethodStartsAt == -1)
            return null
        /*
         * ...
         * public void testMethodName() { // <- `testMethodStartsAt`
         *     ...
         *     className.methodName(...) // <- needed `startLine`
         *     ...
         * }
         */

        // searching needed method call
        // Regex("[^:]*") satisfies every character except ':'
        // It is necessary to avoid strings from the stacktrace, such as "className.methodName(FileName.java:10)"
        val publicMethodCallPattern = Regex("""$methodName\([^:]*\)""")
        val privateMethodCallPattern = Regex("""$methodName.*\.invoke\([^:]*\)""") // using reflection
        val methodCallShiftInTestMethod = testsBodyLines
            .drop(testMethodStartsAt + 1) // for search after it
            .indexOfFirst { line ->
                line.contains(publicMethodCallPattern) || line.contains(privateMethodCallPattern)
            }
        if (methodCallShiftInTestMethod == -1)
            return null

        // `startLine` consists of:
        //     shift to the testMethod call (+ testMethodStartsAt)
        //     the line with testMethodName (+ 1)
        //     shift to the method call     (+ methodCallShiftInTestMethod)
        //     to one-based                 (+ 1)
        val startLine = testMethodStartsAt + 1 + methodCallShiftInTestMethod + 1

        return SarifPhysicalLocation(
            SarifArtifact(sourceFinding.testsRelativePath),
            SarifRegion.withStartLine(generatedTestsCode, startLine)
        )
    }

    private val maxUtModelPreviewLength = 20

    private fun UtModel.preview(): String {
        val preview = this.toString()
        if (preview.length <= maxUtModelPreviewLength)
            return preview
        // UtModel is too long to show in preview

        val previewType = "<${this.classId.simpleName}>"
        if (previewType.length <= maxUtModelPreviewLength)
            return previewType
        // UtModel's type is also too long

        return "..."
    }

    /**
     * Returns the number of the last line in the execution path.
     */
    private fun getLastLineNumber(utExecution: UtExecution): Int? {
        // if for some reason we can't extract the last line from the path
        val lastCoveredInstruction =
            utExecution.coverage?.coveredInstructions?.lastOrNull()?.lineNumber

        return if (utExecution is UtSymbolicExecution) {
            val lastPathLine = try {
                utExecution.path.lastOrNull()?.stmt?.javaSourceStartLineNumber
            } catch (t: Throwable) {
                null
            }

            lastPathLine ?: lastCoveredInstruction
        } else {
            lastCoveredInstruction
        }
    }

    private fun shouldProcessExecutionResult(result: UtExecutionResult): Boolean {
        val implicitlyThrown = result is UtImplicitlyThrownException
        val overflowFailure = result is UtOverflowFailure && UtSettings.treatOverflowAsError
        return implicitlyThrown || overflowFailure
    }
}