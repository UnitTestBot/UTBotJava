package org.utbot.summary

import com.github.javaparser.ast.body.MethodDeclaration
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtExecutionCluster
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.analysis.ExecutionStructureAnalysis
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.ast.SourceCodeParser
import org.utbot.summary.comment.cluster.SymbolicExecutionClusterCommentBuilder
import org.utbot.summary.comment.classic.symbolic.SimpleCommentBuilder
import org.utbot.summary.name.SimpleNameBuilder
import java.io.File
import java.nio.file.Path
import mu.KotlinLogging
import org.utbot.common.measureTime
import org.utbot.common.info
import org.utbot.framework.SummariesGenerationType.*
import org.utbot.framework.UtSettings.enableClusterCommentsGeneration
import org.utbot.framework.UtSettings.enableJavaDocGeneration
import org.utbot.framework.UtSettings.useDisplayNameArrowStyle
import org.utbot.framework.UtSettings.enableDisplayNameGeneration
import org.utbot.framework.UtSettings.enableTestNamesGeneration
import org.utbot.framework.UtSettings.summaryGenerationType
import org.utbot.framework.UtSettings.useCustomJavaDocTags
import org.utbot.framework.plugin.api.util.isConstructor
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.summary.fuzzer.names.MethodBasedNameSuggester
import org.utbot.summary.fuzzer.names.ModelBasedNameSuggester
import org.utbot.summary.comment.customtags.symbolic.CustomJavaDocCommentBuilder
import soot.SootMethod

private val logger = KotlinLogging.logger {}

fun Collection<UtMethodTestSet>.summarizeAll(searchDirectory: Path, sourceFile: File?): List<UtMethodTestSet> = logger.info().measureTime({
    "----------------------------------------------------------------------------------------\n" +
    "-------------------Summarization started for ${this.size} test cases--------------------\n" +
    "----------------------------------------------------------------------------------------"
    }) {
    this.map {
        it.summarizeOne(searchDirectory, sourceFile)
    }
}

private fun UtMethodTestSet.summarizeOne(searchDirectory: Path, sourceFile: File?): UtMethodTestSet = logger.info().measureTime({ "Summarization for ${this.method}"} ){
    if (summaryGenerationType == NONE) return this

    val sourceFileToAnalyze = sourceFile
        ?: when (summaryGenerationType) {
            FULL -> Instrumenter.adapter.computeSourceFileByClass(this.method.classId.jClass, searchDirectory)
            LIGHT,
            NONE -> null
        }

    makeDiverseExecutions(this)

    // HACK: we avoid calling [invokeDescriptions] method to save time, it is useless in Contest
    val invokeDescriptions = when (summaryGenerationType) {
        FULL -> invokeDescriptions(this, searchDirectory)
        LIGHT,
        NONE -> emptyList()
    }

    // every cluster has summary and list of executions
    val executionClusters = Summarization(sourceFileToAnalyze, invokeDescriptions).fillSummaries(this)
    val updatedExecutions = executionClusters.flatMap { it.executions }
    var pos = 0
    val clustersInfo = executionClusters.map {
        val clusterSize = it.executions.size
        val indices = pos until (pos + clusterSize)
        pos += clusterSize
        it.clusterInfo to indices
    }
    return this.copy(
            executions = updatedExecutions,
            clustersInfo = clustersInfo
        ) // TODO: looks weird and don't create the real copy
}

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

        val executionClusters = mutableListOf<UtExecutionCluster>()

        when (summaryGenerationType) {
            FULL -> {
                executionClusters += generateSummariesForTestsWithNonEmptyPathsProducedBySymbolicExecutor(testSet)
                executionClusters += generateFuzzerBasedSummariesForTests(testSet)
                executionClusters += generateSummariesForTestsWithEmptyPathsProducedBySymbolicExecutor(testSet)
            }
            LIGHT -> {
                executionClusters += generateFuzzerBasedSummariesForTests(testSet, MethodDescriptionSource.SYMBOLIC)
                executionClusters += generateFuzzerBasedSummariesForTests(testSet)
            }
            NONE -> error("We must not fill summaries if SummariesGenerationType is NONE")
        }

        return if (enableClusterCommentsGeneration && executionClusters.size > 0)
            executionClusters
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
                val clusterHeader = clusterTraceTags.clusterHeader.takeIf { enableClusterCommentsGeneration }
                val clusterContent = if (
                    enableClusterCommentsGeneration && clusterTraceTags.isSuccessful // add only for successful executions
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
                    if (enableJavaDocGeneration) {
                        if (useCustomJavaDocTags) {
                            traceTags.execution.summary =
                                CustomJavaDocCommentBuilder(traceTags, sootToAST).buildDocStatements(methodUnderTest)
                        } else {
                            traceTags.execution.summary =
                                SimpleCommentBuilder(traceTags, sootToAST).buildDocStmts(methodUnderTest)
                        }
                    }

                    if (enableDisplayNameGeneration || enableTestNamesGeneration) {
                        val simpleNameBuilder = SimpleNameBuilder(traceTags, sootToAST, methodUnderTest)
                        val name = simpleNameBuilder.name
                        val displayName = simpleNameBuilder.displayName
                        val fromToName = simpleNameBuilder.fromToName
                        val nameIndex = namesCounter.getOrPut(name) { 0 }
                        namesCounter[name] = nameIndex + 1
                        updatedExecutions += traceTags.execution
                        if (enableDisplayNameGeneration) {
                            if (!useDisplayNameArrowStyle) {
                                traceTags.execution.displayName = displayName
                            } else {
                                traceTags.execution.displayName = fromToName
                            }
                        }
                        if (enableTestNamesGeneration) {
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

    private fun generateFuzzerBasedSummariesForTests(
        testSet: UtMethodTestSet,
        descriptionSource: MethodDescriptionSource = MethodDescriptionSource.FUZZER
    ): List<UtExecutionCluster> {
        val clustersToReturn: MutableList<UtExecutionCluster> = mutableListOf()
        val methodTestSet = when (descriptionSource) {
                MethodDescriptionSource.FUZZER -> prepareTestSetWithFuzzedExecutions(testSet)
                MethodDescriptionSource.SYMBOLIC -> prepareTestSetForByteCodeAnalysis(testSet)
        }

        if (methodTestSet.executions.isNotEmpty()) {
            methodTestSet.executions.forEach { utExecution ->
                val nameSuggester = sequenceOf(ModelBasedNameSuggester(), MethodBasedNameSuggester(descriptionSource))
                val testMethodName = try {
                    nameSuggester.flatMap {
                        when (descriptionSource) {
                            MethodDescriptionSource.FUZZER -> {
                                with(utExecution as UtFuzzedExecution) {
                                    it.suggest(
                                        utExecution.fuzzedMethodDescription as FuzzedMethodDescription,
                                        utExecution.fuzzingValues as List<FuzzedValue>,
                                        utExecution.result
                                    )
                                }
                            }

                            MethodDescriptionSource.SYMBOLIC -> {
                                val executableId = testSet.method
                                val description = FuzzedMethodDescription(executableId).apply {
                                    compilableName = if (!executableId.isConstructor) executableId.name else null
                                }
                                it.suggest(
                                    description,
                                    utExecution.stateBefore.parameters.map { value -> FuzzedValue(value) },
                                    utExecution.result
                                )
                            }
                        }
                    }.firstOrNull()
                } catch (t: Throwable) {
                    logger.error(t) { "Cannot create suggested test name for $utExecution" } // TODO: add better explanation or default behaviour
                    null
                }

                utExecution.testMethodName = testMethodName?.testName
                utExecution.displayName = testMethodName?.displayName
                utExecution.summary = testMethodName?.javaDoc
            }

            val clusteredExecutions = groupFuzzedExecutions(methodTestSet)
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

    /** Filter and copies fuzzed executions. */
    private fun prepareTestSetWithFuzzedExecutions(testSet: UtMethodTestSet): UtMethodTestSet {
        val executions = testSet.executions.filterIsInstance<UtFuzzedExecution>()

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

    /** ASTs of invokes are also included. */
    private fun sootToAST(
        testSet: UtMethodTestSet
    ): MutableMap<SootMethod, JimpleToASTMap>? {
        val sootToAST = mutableMapOf<SootMethod, JimpleToASTMap>()
        val jimpleBody = testSet.jimpleBody
        if (jimpleBody == null) {
            logger.debug { "No jimple body of method under test ${testSet.method.name}." }
            return null
        }

        if (sourceFile != null && sourceFile.exists()) {
            val methodUnderTestAST = SourceCodeParser(sourceFile, testSet).methodAST

            if (methodUnderTestAST == null) {
                logger.debug { "Couldn't parse source file with path ${sourceFile.absolutePath} of method under test ${testSet.method.name}." }
                return null
            }

            sootToAST[jimpleBody.method] = JimpleToASTMap(jimpleBody.units, methodUnderTestAST)
            invokeDescriptions.forEach {
                sootToAST[it.sootMethod] = JimpleToASTMap(it.sootMethod.jimpleBody().units, it.ast)
            }
            return sootToAST
        } else {
            logger.debug { "Couldn't find source file of method under test ${testSet.method.name}." }
            return null
        }
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
            val methodFile = Instrumenter.adapter.computeSourceFileByNameAndPackage(
                sootMethod.declaringClass.name,
                sootMethod.declaringClass.javaPackageName.replace(".", File.separator),
                searchDirectory
            )

            if (methodFile != null && methodFile.exists()) {
                val ast = methodFile.let {
                    SourceCodeParser(sootMethod, it).methodAST
                }
                if (ast != null) InvokeDescription(sootMethod, ast) else null
            } else {
                null
            }
        }
}

data class InvokeDescription(val sootMethod: SootMethod, val ast: MethodDeclaration)

/**
 * Sometimes, we need to use fuzzer for preparing summaries even for [UtSymbolicExecution]s.
 * See [Summarization.generateFuzzerBasedSummariesForTests].
 */
enum class MethodDescriptionSource {
    FUZZER,
    SYMBOLIC,
}