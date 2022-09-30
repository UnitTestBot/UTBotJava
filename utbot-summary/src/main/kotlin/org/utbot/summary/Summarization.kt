package org.utbot.summary

import com.github.javaparser.ast.body.MethodDeclaration
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtExecutionCluster
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.UtSummarySettings.GENERATE_CLUSTER_COMMENTS
import org.utbot.summary.UtSummarySettings.GENERATE_COMMENTS
import org.utbot.summary.UtSummarySettings.GENERATE_DISPLAYNAME_FROM_TO_STYLE
import org.utbot.summary.UtSummarySettings.GENERATE_DISPLAY_NAMES
import org.utbot.summary.UtSummarySettings.GENERATE_NAMES
import org.utbot.summary.analysis.ExecutionStructureAnalysis
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.ast.SourceCodeParser
import org.utbot.summary.comment.SymbolicExecutionClusterCommentBuilder
import org.utbot.summary.comment.SimpleCommentBuilder
import org.utbot.summary.name.SimpleNameBuilder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import mu.KotlinLogging
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.util.humanReadableName
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.summary.fuzzer.names.MethodBasedNameSuggester
import org.utbot.summary.fuzzer.names.ModelBasedNameSuggester
import org.utbot.summary.comment.CustomJavaDocCommentBuilder
import soot.SootMethod

private val logger = KotlinLogging.logger {}

fun UtMethodTestSet.summarize(sourceFile: File?, searchDirectory: Path = Paths.get("")): UtMethodTestSet {
    if (!UtSettings.enableMachineLearningModule) return this

    return try {
        makeDiverseExecutions(this)
        val invokeDescriptions = invokeDescriptions(this, searchDirectory)
        // every cluster has summary and list of executions
        val executionClusters = Summarization(sourceFile, invokeDescriptions).fillSummaries(this)
        val updatedExecutions = executionClusters.flatMap { it.executions }
        var pos = 0
        val clustersInfo = executionClusters.map {
            val clusterSize = it.executions.size
            val indices = pos until (pos + clusterSize)
            pos += clusterSize
            it.clusterInfo to indices
        }
        this.copy(
            executions = updatedExecutions,
            clustersInfo = clustersInfo
        ) // TODO: looks weird and don't create the real copy
    } catch (e: Throwable) {
        logger.info(e) { "Summary generation error: ${e.message}" }
        this
    }
}

fun UtMethodTestSet.summarize(searchDirectory: Path): UtMethodTestSet =
    this.summarize(Instrumenter.computeSourceFileByClass(this.method.classId.jClass, searchDirectory), searchDirectory)


class Summarization(val sourceFile: File?, val invokeDescriptions: List<InvokeDescription>) {
    private val tagGenerator = TagGenerator()
    private val jimpleBodyAnalysis = ExecutionStructureAnalysis()

    fun fillSummaries(testSet: UtMethodTestSet): List<UtExecutionCluster> {
        if (testSet.executions.isEmpty()) {
            logger.info {
                "No execution traces found in test case " +
                        "for method ${testSet.method.classId.name}, " + "${testSet.jimpleBody}"
            }
            return listOf(UtExecutionCluster(UtClusterInfo(), testSet.executions))
        }

        val clustersToReturn = mutableListOf<UtExecutionCluster>()

        clustersToReturn += generateSummariesForTestsWithNonEmptyPathsProducedBySymbolicExecutor(testSet)
        clustersToReturn += generateSummariesForTestsProducedByFuzzer(testSet)
        clustersToReturn += generateSummariesForTestsWithEmptyPathsProducedBySymbolicExecutor(testSet)

        return if (clustersToReturn.size > 0)
            clustersToReturn
        else
            listOf(UtExecutionCluster(UtClusterInfo(), testSet.executions))
    }

    private fun generateSummariesForTestsWithNonEmptyPathsProducedBySymbolicExecutor(
        testSet: UtMethodTestSet
    ): List<UtExecutionCluster> {
        val clustersToReturn: MutableList<UtExecutionCluster> = mutableListOf()
        val testSetWithNonEmptyPaths = prepareTestSetForByteCodeAnalysis(testSet)

        val sootToAST = sootToAST(testSetWithNonEmptyPaths)
        val jimpleBody = testSet.jimpleBody
        val updatedExecutions = mutableListOf<UtSymbolicExecution>()
        val namesCounter = mutableMapOf<String, Int>()

        // analyze
        if (jimpleBody != null && sootToAST != null) {
            val methodUnderTest = jimpleBody.method
            val clusteredTags = tagGenerator.testSetToTags(testSetWithNonEmptyPaths)
            jimpleBodyAnalysis.traceStructuralAnalysis(jimpleBody, clusteredTags, methodUnderTest, invokeDescriptions)
            val numberOfSuccessfulClusters = clusteredTags.filter { it.isSuccessful }.size
            for (clusterTraceTags in clusteredTags) {
                val clusterHeader = clusterTraceTags.clusterHeader.takeIf { GENERATE_CLUSTER_COMMENTS }
                val clusterContent = if (
                    GENERATE_CLUSTER_COMMENTS && clusterTraceTags.isSuccessful // add only for successful executions
                    && numberOfSuccessfulClusters > 1 // there is more than one successful execution
                    && clusterTraceTags.traceTags.size > 1 // add if there is more than 1 execution
                ) {
                    SymbolicExecutionClusterCommentBuilder(clusterTraceTags.commonStepsTraceTag, sootToAST)
                        .buildString(methodUnderTest)
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            buildString {
                                append("${NEW_LINE}Common steps:")
                                append("$NEW_LINE$it")
                            }
                        }
                } else {
                    null // TODO: handle it correctly, something like common cluster or something else
                }

                for (traceTags in clusterTraceTags.traceTags) {
                    if (GENERATE_COMMENTS) {
                        if (UtSettings.useCustomJavaDocTags) {
                            traceTags.execution.summary =
                                CustomJavaDocCommentBuilder(traceTags, sootToAST).buildDocStatements(methodUnderTest)
                        } else {
                            traceTags.execution.summary =
                                SimpleCommentBuilder(traceTags, sootToAST).buildDocStmts(methodUnderTest)
                        }
                    }

                    if (GENERATE_DISPLAY_NAMES || GENERATE_NAMES) {
                        val simpleNameBuilder = SimpleNameBuilder(traceTags, sootToAST, methodUnderTest)
                        val name = simpleNameBuilder.name
                        val displayName = simpleNameBuilder.displayName
                        val fromToName = simpleNameBuilder.fromToName
                        val nameIndex = namesCounter.getOrPut(name) { 0 }
                        namesCounter[name] = nameIndex + 1
                        updatedExecutions += traceTags.execution
                        if (GENERATE_DISPLAY_NAMES) {
                            if (!GENERATE_DISPLAYNAME_FROM_TO_STYLE) {
                                traceTags.execution.displayName = displayName
                            } else {
                                traceTags.execution.displayName = fromToName
                            }
                        }
                        if (GENERATE_NAMES) {
                            traceTags.execution.testMethodName = name
                            if (nameIndex != 0) traceTags.execution.testMethodName += "_$nameIndex"
                        }
                    }

                    logger.debug { "Summary:\n${traceTags.execution.summary}" }
                    logger.debug { "Name: ${traceTags.execution.testMethodName}" }
                }
                clustersToReturn.add(
                    UtExecutionCluster(
                        UtClusterInfo(clusterHeader, clusterContent),
                        clusterTraceTags.traceTags.map { it.execution }
                    )
                )
            }

            return clustersToReturn.toList()
        }

        // if there is no Jimple body or no AST, return one cluster with empty summary and all executions
        return listOf(UtExecutionCluster(UtClusterInfo(), testSet.executions))
    }

    private fun generateSummariesForTestsWithEmptyPathsProducedBySymbolicExecutor(
        testSet: UtMethodTestSet,
    ): List<UtExecutionCluster> {
        val clustersToReturn: MutableList<UtExecutionCluster> = mutableListOf()
        val testSetWithEmptyPaths = prepareTestSetWithEmptyPaths(testSet)

        val executionsWithEmptyPaths = testSetWithEmptyPaths.executions

        if (executionsWithEmptyPaths.isNotEmpty()) {
            executionsWithEmptyPaths.forEach {
                logger.info {
                    "The path for test ${it.testMethodName} " +
                            "for method ${testSet.method.classId.name} is empty and summaries could not be generated."
                }
            }

            val clusteredExecutions = groupExecutionsWithEmptyPaths(testSetWithEmptyPaths)

            clusteredExecutions.forEach {
                clustersToReturn.add(
                    UtExecutionCluster(
                        UtClusterInfo(it.header),
                        it.executions
                    )
                )
            }
        }
        return clustersToReturn.toList()
    }

    private fun generateSummariesForTestsProducedByFuzzer(
        testSet: UtMethodTestSet
    ): List<UtExecutionCluster> {
        val clustersToReturn: MutableList<UtExecutionCluster> = mutableListOf()
        val executionsProducedByFuzzer = testSet.executions.filterIsInstance<UtFuzzedExecution>()
        val successfulFuzzerExecutions = mutableListOf<UtFuzzedExecution>()
        val unsuccessfulFuzzerExecutions = mutableListOf<UtFuzzedExecution>()

        if (executionsProducedByFuzzer.isNotEmpty()) {
            executionsProducedByFuzzer.forEach { utExecution ->

                val nameSuggester = sequenceOf(ModelBasedNameSuggester(), MethodBasedNameSuggester())
                val testMethodName = try {
                    nameSuggester.flatMap {
                        it.suggest(
                            utExecution.fuzzedMethodDescription as FuzzedMethodDescription,
                            utExecution.fuzzingValues as List<FuzzedValue>,
                            utExecution.result
                        )
                    }.firstOrNull()
                } catch (t: Throwable) {
                    logger.error(t) { "Cannot create suggested test name for $utExecution" } // TODO: add better explanation or default behavoiur
                    null
                }

                utExecution.testMethodName = testMethodName?.testName
                utExecution.displayName = testMethodName?.displayName

                when (utExecution.result) {
                    is UtConcreteExecutionFailure -> unsuccessfulFuzzerExecutions.add(utExecution)
                    is UtExplicitlyThrownException -> unsuccessfulFuzzerExecutions.add(utExecution)
                    is UtImplicitlyThrownException -> unsuccessfulFuzzerExecutions.add(utExecution)
                    is UtOverflowFailure -> unsuccessfulFuzzerExecutions.add(utExecution)
                    is UtSandboxFailure -> unsuccessfulFuzzerExecutions.add(utExecution)
                    is UtTimeoutException -> unsuccessfulFuzzerExecutions.add(utExecution)
                    is UtExecutionSuccess -> successfulFuzzerExecutions.add(utExecution)
                }
            }

            if (successfulFuzzerExecutions.isNotEmpty()) {
                val clusterHeader = buildFuzzerClusterHeaderForSuccessfulExecutions(testSet)

                clustersToReturn.add(
                    UtExecutionCluster(
                        UtClusterInfo(clusterHeader, null),
                        successfulFuzzerExecutions
                    )
                )
            }

            if (unsuccessfulFuzzerExecutions.isNotEmpty()) {
                val clusterHeader = buildFuzzerClusterHeaderForUnsuccessfulExecutions(testSet)

                clustersToReturn.add(
                    UtExecutionCluster(
                        UtClusterInfo(clusterHeader, null),
                        unsuccessfulFuzzerExecutions
                    )
                )
            }
        }

        return clustersToReturn.toList()
    }

    private fun buildFuzzerClusterHeaderForSuccessfulExecutions(testSet: UtMethodTestSet): String {
        val commentPrefix = "FUZZER:"
        val commentPostfix = "for method ${testSet.method.humanReadableName}"

        return "$commentPrefix ${ExecutionGroup.SUCCESSFUL_EXECUTIONS.displayName} $commentPostfix"
    }

    private fun buildFuzzerClusterHeaderForUnsuccessfulExecutions(testSet: UtMethodTestSet): String {
        val commentPrefix = "FUZZER:"
        val commentPostfix = "for method ${testSet.method.humanReadableName}"

        return "$commentPrefix ${ExecutionGroup.EXPLICITLY_THROWN_UNCHECKED_EXCEPTIONS} $commentPostfix"
    }

    /** Filter and copies executions with non-empty paths. */
    private fun prepareTestSetForByteCodeAnalysis(testSet: UtMethodTestSet): UtMethodTestSet {
        val executions =
            testSet.executions.filterIsInstance<UtSymbolicExecution>()
                .filter { it.path.isNotEmpty() }

        return UtMethodTestSet(
            method = testSet.method,
            executions = executions,
            jimpleBody = testSet.jimpleBody,
            errors = testSet.errors,
            clustersInfo = testSet.clustersInfo
        )
    }

    /** Filter and copies executions with non-empty paths. */
    private fun prepareTestSetWithEmptyPaths(testSet: UtMethodTestSet): UtMethodTestSet {
        val executions =
            testSet.executions.filterIsInstance<UtSymbolicExecution>()
                .filter { it.path.isEmpty() }

        return UtMethodTestSet(
            method = testSet.method,
            executions = executions,
            jimpleBody = testSet.jimpleBody,
            errors = testSet.errors,
            clustersInfo = testSet.clustersInfo
        )
    }

    /*
    * asts of invokes also included
    * */
    private fun sootToAST(
        testSet: UtMethodTestSet
    ): MutableMap<SootMethod, JimpleToASTMap>? {
        val sootToAST = mutableMapOf<SootMethod, JimpleToASTMap>()
        val jimpleBody = testSet.jimpleBody
        if (jimpleBody == null) {
            logger.info { "No jimple body of method under test" }
            return null
        }
        val methodUnderTestAST = sourceFile?.let {
            SourceCodeParser(it, testSet).methodAST
        }

        if (methodUnderTestAST == null) {
            logger.info { "Couldn't parse source file of method under test" }
            return null
        }

        sootToAST[jimpleBody.method] = JimpleToASTMap(jimpleBody.units, methodUnderTestAST)
        invokeDescriptions.forEach {
            sootToAST[it.sootMethod] = JimpleToASTMap(it.sootMethod.jimpleBody().units, it.ast)
        }
        return sootToAST
    }
}

private fun makeDiverseExecutions(testSet: UtMethodTestSet) {
    val symbolicExecutions = testSet.executions.filterIsInstance<UtSymbolicExecution>()

    val maxDepth = symbolicExecutions.flatMap { it.path }.maxOfOrNull { it.depth } ?: 0

    if (maxDepth > 0) {
        logger.info { "Recursive function, max recursion: $maxDepth" }
        return
    }

    var diversity = percentageDiverseExecutions(symbolicExecutions)
    if (diversity >= 50) {
        logger.info { "Diversity execution path percentage: $diversity" }
        return
    }

    for (depth in 1..2) {
        logger.info { "Depth to add: $depth" }
        stepsUpToDepth(symbolicExecutions, depth)
        diversity = percentageDiverseExecutions(symbolicExecutions)

        if (diversity >= 50) {
            logger.info { "Diversity execution path percentage: $diversity" }
            return
        }
    }
}

private fun invokeDescriptions(testSet: UtMethodTestSet, searchDirectory: Path): List<InvokeDescription> {
    val sootInvokes =
        testSet.executions.filterIsInstance<UtSymbolicExecution>().flatMap { it.path.invokeJimpleMethods() }.toSet()
    return sootInvokes
        //TODO(SAT-1170)
        .filterNot { "\$lambda" in it.declaringClass.name }
        .mapNotNull { sootMethod ->
            val methodFile = Instrumenter.computeSourceFileByClass(
                sootMethod.declaringClass.name,
                sootMethod.declaringClass.javaPackageName.replace(".", File.separator),
                searchDirectory
            )
            val ast = methodFile?.let {
                SourceCodeParser(sootMethod, it).methodAST
            }
            if (ast != null) InvokeDescription(sootMethod, ast) else null
        }
}

data class InvokeDescription(val sootMethod: SootMethod, val ast: MethodDeclaration)