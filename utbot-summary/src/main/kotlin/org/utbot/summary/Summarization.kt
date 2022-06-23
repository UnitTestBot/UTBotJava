package org.utbot.summary

import com.github.javaparser.ast.body.MethodDeclaration
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionCluster
import org.utbot.framework.plugin.api.UtTestCase
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

fun UtTestCase.summarize(sourceFile: File?, searchDirectory: Path = Paths.get("")): UtTestCase {
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
        this.copy(executions = updatedExecutions, clustersInfo = clustersInfo)
    } catch (e: Throwable) {
        logger.info(e) { "Summary generation error" }
        this
    }
}

fun UtTestCase.summarize(searchDirectory: Path): UtTestCase =
    this.summarize(Instrumenter.computeSourceFileByClass(this.method.clazz.java, searchDirectory), searchDirectory)


class Summarization(val sourceFile: File?, val invokeDescriptions: List<InvokeDescription>) {
    private val tagGenerator = TagGenerator()
    private val jimpleBodyAnalysis = ExecutionStructureAnalysis()

    fun summary(testCase: UtTestCase): List<UtExecutionCluster> {
        val namesCounter = mutableMapOf<String, Int>()

        if (testCase.executions.isEmpty()) {
            logger.info {
                "No execution traces found in test case " +
                        "for method ${testCase.method.clazz.qualifiedName}, " + "${testCase.jimpleBody}"
            }
            return listOf(UtExecutionCluster(UtClusterInfo(), testCase.executions))
        }
        // init
        val sootToAST = sootToAST(testCase)
        val jimpleBody = testCase.jimpleBody
        val updatedExecutions = mutableListOf<UtExecution>()
        val clustersToReturn = mutableListOf<UtExecutionCluster>()

        // analyze
        if (jimpleBody != null && sootToAST != null) {
            val methodUnderTest = jimpleBody.method
            val clusteredTags = tagGenerator.testCaseToTags(testCase)
            jimpleBodyAnalysis.traceStructuralAnalysis(jimpleBody, clusteredTags, methodUnderTest, invokeDescriptions)
            val numberOfSuccessfulClusters = clusteredTags.filter { it.isSuccessful }.size
            for (clusterTraceTags in clusteredTags) {
                val clusterHeader = clusterTraceTags.summary.takeIf { GENERATE_CLUSTER_COMMENTS }
                val clusterContent = if (
                    GENERATE_CLUSTER_COMMENTS && clusterTraceTags.isSuccessful //add only for successful executions
                    && numberOfSuccessfulClusters > 1 //there is more than one successful execution
                    && clusterTraceTags.traceTags.size > 1 //add if there is more than 1 execution
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
                        traceTags.execution.summary = SimpleCommentBuilder(traceTags, sootToAST).buildDocStmts(methodUnderTest)
                    }

                    if (GENERATE_DISPLAY_NAMES || GENERATE_NAMES) {
                        val simpleNameBuilder = SimpleNameBuilder(traceTags, sootToAST, methodUnderTest)
                        val name = simpleNameBuilder.name
                        val displayName = simpleNameBuilder.displayName
                        val fromToName = simpleNameBuilder.fromToName
                        val nameIndex = namesCounter.getOrPut(name) { 0 }
                        namesCounter[name] = nameIndex + 1
                        updatedExecutions += traceTags.execution
                        if (GENERATE_DISPLAY_NAMES
                            // do not rewrite display name if already set
                            && traceTags.execution.displayName.isNullOrBlank()) {
                            if (!GENERATE_DISPLAYNAME_FROM_TO_STYLE) {
                                traceTags.execution.displayName = displayName
                            } else {
                                traceTags.execution.displayName = fromToName
                            }
                        }
                        if (GENERATE_NAMES
                            // do not rewrite display name if already set
                            && traceTags.execution.testMethodName.isNullOrBlank()) {
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
        return listOf(UtExecutionCluster(UtClusterInfo(), testCase.executions))
    }

    /*
    * asts of invokes also included
    * */
    private fun sootToAST(
        testCase: UtTestCase
    ): MutableMap<SootMethod, JimpleToASTMap>? {
        val sootToAST = mutableMapOf<SootMethod, JimpleToASTMap>()
        val jimpleBody = testCase.jimpleBody
        if (jimpleBody == null) {
            logger.info { "No jimple body of method under test" }
            return null
        }
        val methodUnderTestAST = sourceFile?.let {
            SourceCodeParser(it, testCase).methodAST
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

private fun makeDiverseExecutions(testCase: UtTestCase) {
    val maxDepth = testCase.executions.flatMap { it.path }.maxOfOrNull { it.depth } ?: 0

    if (maxDepth > 0) {
        logger.info { "Recursive function, max recursion: $maxDepth" }
        return
    }

    var diversity = percentageDiverseExecutions(testCase.executions)
    if (diversity >= 50) {
        logger.info { "Diversity execution path percentage: $diversity" }
        return
    }

    for (depth in 1..2) {
        logger.info { "Depth to add: $depth" }
        stepsUpToDepth(testCase.executions, depth)
        diversity = percentageDiverseExecutions(testCase.executions)

        if (diversity >= 50) {
            logger.info { "Diversity execution path percentage: $diversity" }
            return
        }
    }
}

private fun invokeDescriptions(testCase: UtTestCase, seachDirectory: Path): List<InvokeDescription> {
    val sootInvokes = testCase.executions.flatMap { it.path.invokeJimpleMethods() }.toSet()
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