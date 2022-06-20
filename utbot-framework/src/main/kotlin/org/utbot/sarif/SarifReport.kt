package org.utbot.sarif

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.utbot.common.PathUtil.fileExtension
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtTestCase

/**
 * Used for the SARIF report creation by given test cases and generated tests code.
 * SARIF is a JSON-based format for presenting static analyzer results.
 * You can open a report (*.sarif file) with the VS Code extension "Sarif Viewer".
 *
 * [Read more about SARIF rules](https://github.com/microsoft/sarif-tutorials/blob/main/README.md)
 * [Sample report](https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning#example-with-minimum-required-properties)
 */
class SarifReport(
    private val testCases: List<UtTestCase>,
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

    private fun shouldProcessUncheckedException(result: UtExecutionResult) = (result is UtImplicitlyThrownException)
            || ((result is UtOverflowFailure) && UtSettings.treatOverflowAsError)

    private fun constructSarif(): Sarif {
        val sarifResults = mutableListOf<SarifResult>()
        val sarifRules = mutableSetOf<SarifRule>()

        for (testCase in testCases) {
            for (execution in testCase.executions) {
                if (shouldProcessUncheckedException(execution.result)) {
                    val (sarifResult, sarifRule) = processUncheckedException(
                        method = testCase.method,
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
                sarifResults
            )
        )
    }

    private fun processUncheckedException(
        method: UtMethod<*>,
        utExecution: UtExecution,
        uncheckedException: UtExecutionFailure
    ): Pair<SarifResult, SarifRule> {

        val exceptionName = uncheckedException.exception::class.java.simpleName
        val ruleId = "utbot.unchecked.$exceptionName"

        val methodName = method.callable.name
        val classFqn = method.clazz.qualifiedName
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
        val sourceRegion = SarifRegion(
            startLine = extractLineNumber(utExecution) ?: defaultLineNumber
        )
        return listOf(
            SarifPhysicalLocationWrapper(
                SarifPhysicalLocation(SarifArtifact(sourceRelativePath), sourceRegion)
            )
        )
    }

    private fun getRelatedLocations(utExecution: UtExecution): List<SarifRelatedPhysicalLocationWrapper> {
        val lineNumber = generatedTestsCode.split('\n').indexOfFirst { line ->
            utExecution.testMethodName?.let { testMethodName ->
                line.contains(testMethodName)
            } ?: false
        }
        val sourceRegion = SarifRegion(
            startLine = if (lineNumber != -1) lineNumber + 1 else defaultLineNumber
        )
        return listOf(
            SarifRelatedPhysicalLocationWrapper(
                relatedLocationId,
                SarifPhysicalLocation(SarifArtifact(sourceFinding.testsRelativePath), sourceRegion)
            )
        )
    }

    private fun getCodeFlows(
        method: UtMethod<*>,
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
            it.className == method.clazz.qualifiedName && it.methodName == method.callable.name
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
            findMethodCallInTestBody(utExecution.testMethodName, method.callable.name)
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
        return SarifFlowLocationWrapper(
            SarifFlowLocation(
                message = Message(
                    text = stackTraceElement.toString()
                ),
                physicalLocation = SarifPhysicalLocation(
                    SarifArtifact(relativePath),
                    SarifRegion(lineNumber)
                )
            )
        )
    }

    private fun findMethodCallInTestBody(testMethodName: String?, methodName: String): SarifPhysicalLocation? {
        if (testMethodName == null)
            return null

        // searching needed test
        val testsBodyLines = generatedTestsCode.split('\n')
        val testMethodStartsAt = testsBodyLines.indexOfFirst { line ->
            line.contains(testMethodName)
        }
        if (testMethodStartsAt == -1)
            return null

        // searching needed method call
        val publicMethodCall = "$methodName("
        val privateMethodCall = Regex("""$methodName.*\.invoke\(""") // using reflection
        val methodCallLineNumber = testsBodyLines
            .drop(testMethodStartsAt + 1) // for search after it
            .indexOfFirst { line ->
                line.contains(publicMethodCall) || line.contains(privateMethodCall)
            }
        if (methodCallLineNumber == -1)
            return null

        return SarifPhysicalLocation(
            SarifArtifact(sourceFinding.testsRelativePath),
            SarifRegion(startLine = methodCallLineNumber + 1 + testMethodStartsAt + 1)
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

    private fun extractLineNumber(utExecution: UtExecution): Int? =
        try {
            utExecution.path.lastOrNull()?.stmt?.javaSourceStartLineNumber
        } catch (t: Throwable) {
            null
        }
}