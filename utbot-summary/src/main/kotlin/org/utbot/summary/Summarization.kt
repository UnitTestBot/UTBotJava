package org.utbot.summary

import com.github.javaparser.ast.body.MethodDeclaration
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionCluster
import org.utbot.framework.plugin.api.UtExecutionCreator
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
import org.utbot.summary.comment.SimpleClusterCommentBuilder
import org.utbot.summary.comment.SimpleCommentBuilder
import org.utbot.summary.name.SimpleNameBuilder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import mu.KotlinLogging
import soot.SootMethod

private val logger = KotlinLogging.logger {}

fun UtMethodTestSet.summarize(sourceFile: File?, searchDirectory: Path = Paths.get("")): UtMethodTestSet {
    if (!UtSettings.enableMachineLearningModule) return this

    return try {
        makeDiverseExecutions(this)
        val invokeDescriptions = invokeDescriptions(this, searchDirectory)
        // every cluster has summary and list of executions
        val executionClusters = Summarization(sourceFile, invokeDescriptions).summary(this)
        val updatedExecutions = executionClusters.flatMap { it.executions }
        var pos = 0
        val clustersInfo = executionClusters.map {
            val clusterSize = it.executions.size
            val indices = pos until (pos + clusterSize)
            pos += clusterSize
            it.clusterInfo to indices
        }
        this.copy(executions = updatedExecutions, clustersInfo = clustersInfo) // TODO: looks weird and don't create the real copy
    } catch (e: Throwable) {
        logger.info(e) { "Summary generation error" }
        this
    }
}

fun UtMethodTestSet.summarize(searchDirectory: Path): UtMethodTestSet =
    this.summarize(Instrumenter.computeSourceFileByClass(this.method.clazz.java, searchDirectory), searchDirectory)


class Summarization(val sourceFile: File?, val invokeDescriptions: List<InvokeDescription>) {
    private val tagGenerator = TagGenerator()
    private val jimpleBodyAnalysis = ExecutionStructureAnalysis()

    fun summary(testSet: UtMethodTestSet): List<UtExecutionCluster> {
        val namesCounter = mutableMapOf<String, Int>()

        if (testSet.executions.isEmpty()) {
            logger.info {
                "No execution traces found in test case " +
                        "for method ${testSet.method.clazz.qualifiedName}, " + "${testSet.jimpleBody}"
            }
            return listOf(UtExecutionCluster(UtClusterInfo(), testSet.executions))
        }

        // init
        val sootToAST = sootToAST(testSet)
        val jimpleBody = testSet.jimpleBody
        val updatedExecutions = mutableListOf<UtExecution>()
        val clustersToReturn = mutableListOf<UtExecutionCluster>()

        // handles tests produced by fuzzing
        val executionsProducedByFuzzer = testSet.executions.filter { it.createdBy == UtExecutionCreator.FUZZER }

        if (executionsProducedByFuzzer.isNotEmpty()) {
            executionsProducedByFuzzer.forEach {
                logger.info {
                    "Test is created by Fuzzing. The path for test ${it.testMethodName} " +
                            "for method ${testSet.method.clazz.qualifiedName} is empty and summaries could not be generated."
                }
            }

            clustersToReturn.add(
                UtExecutionCluster(
                    UtClusterInfo(), // TODO: add something https://github.com/UnitTestBot/UTBotJava/issues/430
                    executionsProducedByFuzzer
                )
            )
        }

        // handles tests produced by symbolic engine, but with empty paths
        val executionsWithEmptyPaths = getExecutionsCreatedBySymbolicEngineWithEmptyPath(testSet)

        if (executionsWithEmptyPaths.isNotEmpty()) {
            executionsWithEmptyPaths.forEach {
                logger.info {
                    "Test is created by Symbolic Engine. The path for test ${it.testMethodName} " +
                            "for method ${testSet.method.clazz.qualifiedName} is empty and summaries could not be generated."
                }
            }

            clustersToReturn.add(
                UtExecutionCluster(
                    UtClusterInfo(), // TODO: https://github.com/UnitTestBot/UTBotJava/issues/430
                    executionsWithEmptyPaths
                )
            )
        }

        val testSetForAnalysis = prepareTestSetForByteCodeAnalysis(testSet)

        // analyze
        if (jimpleBody != null && sootToAST != null) {
            val methodUnderTest = jimpleBody.method
            val clusteredTags = tagGenerator.testSetToTags(testSetForAnalysis)
            jimpleBodyAnalysis.traceStructuralAnalysis(jimpleBody, clusteredTags, methodUnderTest, invokeDescriptions)
            val numberOfSuccessfulClusters = clusteredTags.filter { it.isSuccessful }.size
            for (clusterTraceTags in clusteredTags) {
                val clusterHeader = clusterTraceTags.summary.takeIf { GENERATE_CLUSTER_COMMENTS }
                val clusterContent = if (
                    GENERATE_CLUSTER_COMMENTS && clusterTraceTags.isSuccessful // add only for successful executions
                    && numberOfSuccessfulClusters > 1 // there is more than one successful execution
                    && clusterTraceTags.traceTags.size > 1 // add if there is more than 1 execution
                ) {
                    SimpleClusterCommentBuilder(clusterTraceTags.commonStepsTraceTag, sootToAST)
                        .buildString(methodUnderTest)
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            buildString {
                                append("${NEW_LINE}Common steps:")
                                append("$NEW_LINE$it")
                            }
                        }
                } else {
                    null
                }

                for (traceTags in clusterTraceTags.traceTags) {
                    if (GENERATE_COMMENTS) {
                        traceTags.execution.summary =
                            SimpleCommentBuilder(traceTags, sootToAST).buildDocStmts(methodUnderTest)
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

    private fun prepareTestSetForByteCodeAnalysis(testSet: UtMethodTestSet): UtMethodTestSet {
        val executions =
            testSet.executions.filterNot { it.createdBy == UtExecutionCreator.FUZZER || (it.createdBy == UtExecutionCreator.SYMBOLIC_ENGINE && it.path.isEmpty()) }

        return UtMethodTestSet(
            method = testSet.method,
            executions = executions,
            jimpleBody = testSet.jimpleBody,
            errors = testSet.errors,
            clustersInfo = testSet.clustersInfo
        )
    }

    private fun getExecutionsCreatedBySymbolicEngineWithEmptyPath(testSet: UtMethodTestSet) =
        testSet.executions.filter { it.createdBy == UtExecutionCreator.SYMBOLIC_ENGINE && it.path.isEmpty() }

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
    val maxDepth = testSet.executions.flatMap { it.path }.maxOfOrNull { it.depth } ?: 0

    if (maxDepth > 0) {
        logger.info { "Recursive function, max recursion: $maxDepth" }
        return
    }

    var diversity = percentageDiverseExecutions(testSet.executions)
    if (diversity >= 50) {
        logger.info { "Diversity execution path percentage: $diversity" }
        return
    }

    for (depth in 1..2) {
        logger.info { "Depth to add: $depth" }
        stepsUpToDepth(testSet.executions, depth)
        diversity = percentageDiverseExecutions(testSet.executions)

        if (diversity >= 50) {
            logger.info { "Diversity execution path percentage: $diversity" }
            return
        }
    }
}

private fun invokeDescriptions(testSet: UtMethodTestSet, seachDirectory: Path): List<InvokeDescription> {
    val sootInvokes = testSet.executions.flatMap { it.path.invokeJimpleMethods() }.toSet()
    return sootInvokes
        //TODO(SAT-1170)
        .filterNot { "\$lambda" in it.declaringClass.name }
        .mapNotNull { sootMethod ->
            val methodFile = Instrumenter.computeSourceFileByClass(
                sootMethod.declaringClass.name,
                sootMethod.declaringClass.javaPackageName.replace(".", File.separator),
                seachDirectory
            )
            val ast = methodFile?.let {
                SourceCodeParser(sootMethod, it).methodAST
            }
            if (ast != null) InvokeDescription(sootMethod, ast) else null
        }
}

data class InvokeDescription(val sootMethod: SootMethod, val ast: MethodDeclaration)