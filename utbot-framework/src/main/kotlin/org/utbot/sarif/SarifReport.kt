package org.utbot.sarif

import org.utbot.common.PathUtil.fileExtension
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

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
                sarif.copy(runs = sarif.runs + Sarif.fromJson(report).runs)
            }.toJson()

        /**
         * Minimizes SARIF results between several reports.
         *
         * More complex version of the [SarifReport.minimizeResults].
         */
        fun minimizeSarifResults(srcPathToSarif: MutableMap<Path, Sarif>): MutableMap<Path, Sarif> {
            val pathToSarifResult = srcPathToSarif.entries.flatMap { (path, sarif) ->
                sarif.getAllResults().map { sarifResult ->
                    path to sarifResult
                }
            }
            val groupedResults = pathToSarifResult.groupBy { (_, sarifResult) ->
                sarifResult.ruleId to sarifResult.locations
            }
            val minimizedResults = groupedResults.map { (_, sarifResultsGroup) ->
                sarifResultsGroup.minByOrNull { (_, sarifResult) ->
                    sarifResult.totalCodeFlowLocations()
                }!!
            }
            val groupedByPath = minimizedResults
                .groupBy { (path, _) -> path }
                .mapValues { (_, resultsWithPath) ->
                    resultsWithPath.map { (_, sarifResult) -> sarifResult } // remove redundant path
                }
            val pathToSarifTool = srcPathToSarif.mapValues { (_, sarif) ->
                sarif.runs.first().tool
            }
            val paths = pathToSarifTool.keys intersect groupedByPath.keys
            return paths.associateWith { path ->
                val sarifTool = pathToSarifTool[path]!!
                val sarifResults = groupedByPath[path]!!
                Sarif.fromRun(SarifRun(sarifTool, sarifResults))
            }.toMutableMap()
        }
    }

    /**
     * Creates a SARIF report.
     */
    fun createReport(): Sarif {
        val sarifResults = mutableListOf<SarifResult>()
        val sarifRules = mutableSetOf<SarifRule>()

        for (testSet in testSets) {
            for (execution in testSet.executions) {
                if (shouldProcessExecutionResult(execution.result)) {
                    val (sarifResult, sarifRule) = processExecutionFailure(
                        method = testSet.method,
                        utExecution = execution,
                        executionFailure = execution.result as UtExecutionFailure
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

    // internal

    private val defaultLineNumber = 1 // if the line in the source code where the exception is thrown is unknown

    /**
     * [Read more about links to locations](https://github.com/microsoft/sarif-tutorials/blob/main/docs/3-Beyond-basics.md#msg-links-location)
     */
    private val relatedLocationId = 1 // for attaching link to generated test in related locations

    private val stackTraceLengthForStackOverflow = 50 // stack overflow error may have too many elements

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

    private fun processExecutionFailure(
        method: ExecutableId,
        utExecution: UtExecution,
        executionFailure: UtExecutionFailure
    ): Pair<SarifResult, SarifRule> {

        val exceptionName = executionFailure.exception::class.java.simpleName
        val ruleId = "utbot.exception.$exceptionName"

        val methodName = method.name
        val classFqn = method.classId.name
        val methodArguments = utExecution.stateBefore.parameters
            .joinToString(prefix = "", separator = ", ", postfix = "") { it.preview() }

        val errorMessage = if (executionFailure is UtTimeoutException)
            "Unexpected behavior: ${executionFailure.exception.message}"
        else
            "Unexpected exception: ${executionFailure.exception}"

        val sarifResult = SarifResult(
            ruleId,
            Level.Error,
            Message(
                text = """
                    $errorMessage.
                    Test case: `$methodName($methodArguments)`
                    [Generated test for this case]($relatedLocationId)
                """.trimIndent()
            ),
            getLocations(method, utExecution, classFqn),
            getRelatedLocations(utExecution),
            getCodeFlows(method, utExecution, executionFailure)
        )
        val sarifRule = SarifRule(
            ruleId,
            exceptionName,
            SarifRule.Description(
                text = "Unexpected $exceptionName detected."
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

    private fun getLocations(
        method: ExecutableId,
        utExecution: UtExecution,
        classFqn: String?
    ): List<SarifLocationWrapper> {
        if (classFqn == null)
            return listOf()
        val (startLine, classWithErrorFqn) = getLastLineNumberWithClassFqn(method, utExecution, classFqn)
        val sourceCode = sourceFinding.getSourceFile(classWithErrorFqn)?.readText() ?: ""
        val sourceRegion = SarifRegion.withStartLine(sourceCode, startLine)
        val sourceRelativePath = sourceFinding.getSourceRelativePath(classWithErrorFqn)
        return listOf(
            SarifPhysicalLocationWrapper(
                SarifPhysicalLocation(SarifArtifact(sourceRelativePath), sourceRegion)
            ),
            SarifLogicalLocationsWrapper(
                listOf(SarifLogicalLocation(classWithErrorFqn)) // class name without method name
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
        executionFailure: UtExecutionFailure
    ): List<SarifCodeFlow> {
        val stackTraceResolved = filterStackTrace(method, utExecution, executionFailure)
            .mapNotNull { findStackTraceElementLocation(it) }
            .toMutableList()

        if (stackTraceResolved.isEmpty()) {
            stackTraceResolved += stackTraceFromCoverage(utExecution) // fallback logic
        }

        // prepending stack trace by `method` call in generated tests
        val methodCallLocation: SarifPhysicalLocation? =
            findMethodCallInTestBody(utExecution.testMethodName, method.name, utExecution)
        if (methodCallLocation != null) {
            val testFileName = sourceFinding.testsRelativePath.toPath().fileName
            val testClassName = testFileName.nameWithoutExtension
            val testMethodName = utExecution.testMethodName
            val methodCallLineNumber = methodCallLocation.region.startLine
            val methodCallLocationWrapper = SarifFlowLocationWrapper(
                SarifFlowLocation(
                    message = Message(
                        text = "$testClassName.$testMethodName($testFileName:$methodCallLineNumber)"
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

    private fun filterStackTrace(
        method: ExecutableId,
        utExecution: UtExecution,
        executionFailure: UtExecutionFailure
    ): List<StackTraceElement> {
        /* Example of a typical stack trace:
            - java.lang.Math.multiplyExact(Math.java:867)
            - com.abc.Util.multiply(Util.java:10)
            - com.abc.Util.multiply(Util.java:6)
            - com.abc.Main.example(Main.java:11) // <- `lastMethodCallIndex`
            - sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            - ...
         */
        var stackTrace = executionFailure.exception.stackTrace.toList()

        val lastMethodCallIndex = stackTrace.indexOfLast {
            it.className == method.classId.name && it.methodName == method.name
        }
        if (lastMethodCallIndex != -1) {
            // taking all elements before the last `method` call
            stackTrace = stackTrace.take(lastMethodCallIndex + 1)
        } else { // no `method` call in the stack trace
            if (executionFailure.exception !is StackOverflowError) {
                stackTrace = listOf() // (likely) the stack trace contains only our internal calls
            }
        }

        if (executionFailure.exception is StackOverflowError) {
            stackTrace = stackTrace.takeLast(stackTraceLengthForStackOverflow)
        }

        val stackTraceFiltered = stackTrace.filter {
            !it.className.startsWith("org.utbot.") // filter all internal calls
        }

        return stackTraceFiltered
    }

    /**
     * Constructs the stack trace from the list of covered instructions.
     */
    private fun stackTraceFromCoverage(utExecution: UtExecution): List<SarifFlowLocationWrapper> {
        val coveredInstructions = utExecution.coverage?.coveredInstructions
            ?: return listOf()

        val executionTrace = coveredInstructions.groupBy { instruction ->
            instruction.className to instruction.methodSignature // group by method
        }.map { (_, instructionsForOneMethod) ->
            instructionsForOneMethod.last() // we need only last to construct the stack trace
        }

        val sarifExecutionTrace = executionTrace.map { instruction ->
            val classFqn = instruction.className.replace('/', '.')
            val methodName = instruction.methodSignature.substringBefore('(')
            val lineNumber = instruction.lineNumber

            val sourceFilePath = sourceFinding.getSourceRelativePath(classFqn)
            val sourceFileName = sourceFilePath.substringAfterLast('/')

            SarifFlowLocationWrapper(SarifFlowLocation(
                message = Message("$classFqn.$methodName($sourceFileName:$lineNumber)"),
                physicalLocation = SarifPhysicalLocation(
                    SarifArtifact(uri = sourceFilePath),
                    SarifRegion(startLine = lineNumber) // lineNumber is one-based
                )
            ))
        }

        return sarifExecutionTrace.reversed() // to stack trace
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

    private fun findMethodCallInTestBody(
        testMethodName: String?,
        methodName: String,
        utExecution: UtExecution,
    ): SarifPhysicalLocation? {
        if (testMethodName == null)
            return null
        if (utExecution.result is UtSandboxFailure) // if there is no method call in test
            return getRelatedLocations(utExecution).firstOrNull()?.physicalLocation

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
     * Returns the number of the last line in the execution path
     * And the name of the class in which it is located.
     */
    private fun getLastLineNumberWithClassFqn(
        method: ExecutableId,
        utExecution: UtExecution,
        defaultClassFqn: String
    ): Pair<Int, String> {
        val coveredInstructions = utExecution.coverage?.coveredInstructions
        val lastCoveredInstruction = coveredInstructions?.lastOrNull()
        if (lastCoveredInstruction != null)
            return Pair(
                lastCoveredInstruction.lineNumber, // .lineNumber is one-based
                lastCoveredInstruction.className.replace('/', '.')
            )

        // if for some reason we can't extract the last line from the coverage
        val lastPathElementLineNumber = try {
            // path/fullPath might be empty when engine executes in another process -
            // soot entities cannot be passed to the main process because kryo cannot deserialize them
            (utExecution as? UtSymbolicExecution)?.path?.lastOrNull()?.stmt?.javaSourceStartLineNumber // one-based
        } catch (t: Throwable) {
            null
        }
        if (lastPathElementLineNumber != null) {
            return Pair(lastPathElementLineNumber, defaultClassFqn)
        }

        val methodDefinitionLine = getMethodDefinitionLineNumber(method)
        return Pair(methodDefinitionLine ?: defaultLineNumber, defaultClassFqn)
    }

    private fun getMethodDefinitionLineNumber(method: ExecutableId): Int? {
        val sourceFile = sourceFinding.getSourceFile(method.classId.canonicalName)
        val lineNumber = sourceFile?.readLines()?.indexOfFirst { line ->
            line.contains(" ${method.name}(") // method definition
        }
        return if (lineNumber == null || lineNumber == -1) null else lineNumber + 1 // to one-based
    }

    private fun shouldProcessExecutionResult(result: UtExecutionResult): Boolean {
        val implicitlyThrown = result is UtImplicitlyThrownException
        val overflowFailure = result is UtOverflowFailure && UtSettings.treatOverflowAsError
        val assertionError = result is UtExplicitlyThrownException && result.exception is AssertionError
        val sandboxFailure = result is UtSandboxFailure
        val timeoutException = result is UtTimeoutException
        return implicitlyThrown || overflowFailure || assertionError || sandboxFailure || timeoutException
    }
}