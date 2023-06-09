package org.utbot.summary

import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtStreamConsumingFailure
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.UtTaintAnalysisFailure
import org.utbot.framework.plugin.api.util.humanReadableName
import org.utbot.framework.plugin.api.util.isCheckedException
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.summary.DBSCANClusteringConstants.MIN_NUMBER_OF_EXECUTIONS_FOR_CLUSTERING
import org.utbot.summary.clustering.MatrixUniqueness
import org.utbot.summary.clustering.SplitSteps
import org.utbot.summary.tag.TraceTag
import org.utbot.summary.tag.TraceTagWithoutExecution

class TagGenerator {
    fun testSetToTags(testSet: UtMethodTestSet): List<TraceTagCluster> {
        val clusteredExecutions = toClusterExecutions(testSet)
        val traceTagClusters = mutableListOf<TraceTagCluster>()

        val numberOfSuccessfulClusters = clusteredExecutions.filterIsInstance<SuccessfulExecutionCluster>().size

        if (clusteredExecutions.isNotEmpty()) {
            val listOfSplitSteps = clusteredExecutions.map {
                val mUniqueness = MatrixUniqueness(it.executions as List<UtSymbolicExecution>)
                mUniqueness.splitSteps()
            }

            // intersections of steps ONLY in successful clusters
            var stepsIntersections = listOf<Step>()

            // we only want to find intersections if there is more than one successful execution
            if (numberOfSuccessfulClusters > 1 && REMOVE_INTERSECTIONS) {
                val commonStepsInSuccessfulEx = listOfSplitSteps
                    .filterIndexed { i, _ -> clusteredExecutions[i] is SuccessfulExecutionCluster } // search only in successful
                    .map { it.commonSteps }
                    .filter { it.isNotEmpty() }
                if (commonStepsInSuccessfulEx.size > 1) {
                    stepsIntersections = commonStepsInSuccessfulEx.first()
                    for (steps in commonStepsInSuccessfulEx) {
                        stepsIntersections = stepsIntersections.intersect(steps).toList()
                    }
                }
            }

            // for every cluster and step add TraceTagCluster
            clusteredExecutions.zip(listOfSplitSteps) { cluster, splitSteps ->
                val commonStepsInCluster =
                    if (stepsIntersections.isNotEmpty() && numberOfSuccessfulClusters > 1) {
                        splitSteps.commonSteps.subtract(stepsIntersections)
                    } else splitSteps.commonSteps

                val splitStepsModified = SplitSteps(
                    uniqueSteps = commonStepsInCluster.toList()
                )

                traceTagClusters.add(
                    TraceTagCluster(
                        cluster.header,
                        generateExecutionTags(cluster.executions as List<UtSymbolicExecution>, splitSteps),
                        TraceTagWithoutExecution(
                            commonStepsInCluster.toList(),
                            cluster.executions.first().result,
                            splitStepsModified
                        ),
                        cluster is SuccessfulExecutionCluster
                    )
                )
            }
        } // clusteredExecutions should not be empty!

        return traceTagClusters
    }
}

/**
 * @return list of TraceTag created from executions and splitsSteps
 */
private fun generateExecutionTags(executions: List<UtSymbolicExecution>, splitSteps: SplitSteps): List<TraceTag> =
    executions.map { TraceTag(it, splitSteps) }


/**
 * Splits executions with empty paths into clusters.
 *
 * @return clustered executions.
 */
fun groupExecutionsWithEmptyPaths(testSet: UtMethodTestSet): List<ExecutionCluster> {
    val methodExecutions = testSet.executions.filterIsInstance<UtSymbolicExecution>()
    val clusters = mutableListOf<ExecutionCluster>()
    val commentPrefix = "OTHER:"
    val commentPostfix = "for method ${testSet.method.humanReadableName}"

    val grouped = methodExecutions.groupBy { it.result.clusterKind() }

    val successfulExecutions = grouped[ExecutionGroup.SUCCESSFUL_EXECUTIONS] ?: emptyList()
    if (successfulExecutions.isNotEmpty()) {
        clusters += SuccessfulExecutionCluster(
            "$commentPrefix ${ExecutionGroup.SUCCESSFUL_EXECUTIONS.displayName} $commentPostfix",
            successfulExecutions.toList()
        )
    }

    clusters += addClustersOfFailedExecutions(grouped, commentPrefix, commentPostfix)
    return clusters
}

/**
 * Splits fuzzed executions into clusters.
 *
 * @return clustered executions.
 */
fun groupFuzzedExecutions(testSet: UtMethodTestSet): List<ExecutionCluster> {
    val methodExecutions = testSet.executions.filterIsInstance<UtFuzzedExecution>()
    val clusters = mutableListOf<ExecutionCluster>()
    val commentPrefix = "FUZZER:"
    val commentPostfix = "for method ${testSet.method.humanReadableName}"

    val grouped = methodExecutions.groupBy { it.result.clusterKind() }

    val successfulExecutions = grouped[ExecutionGroup.SUCCESSFUL_EXECUTIONS] ?: emptyList()
    if (successfulExecutions.isNotEmpty()) {
        clusters += SuccessfulExecutionCluster(
            "$commentPrefix ${ExecutionGroup.SUCCESSFUL_EXECUTIONS.displayName} $commentPostfix",
            successfulExecutions.toList()
        )
    }

    clusters += addClustersOfFailedExecutions(grouped, commentPrefix, commentPostfix)
    return clusters
}

/**
 * Splits symbolic executions into clusters.
 *
 * If Success cluster has more than [MIN_NUMBER_OF_EXECUTIONS_FOR_CLUSTERING] execution
 * then clustering algorithm splits those into more clusters.
 *
 * @return clustered executions.
 */
private fun toClusterExecutions(testSet: UtMethodTestSet): List<ExecutionCluster> {
    val methodExecutions = testSet.executions.filterIsInstance<UtSymbolicExecution>()
    val clusters = mutableListOf<ExecutionCluster>()
    val commentPrefix = "SYMBOLIC EXECUTION:"
    val commentPostfix = "for method ${testSet.method.humanReadableName}"

    val grouped = methodExecutions.groupBy { it.result.clusterKind() }

    val successfulExecutions = grouped[ExecutionGroup.SUCCESSFUL_EXECUTIONS] ?: emptyList()
    if (successfulExecutions.isNotEmpty()) {
        val clustered =
            if (successfulExecutions.size >= MIN_NUMBER_OF_EXECUTIONS_FOR_CLUSTERING) {
                MatrixUniqueness.dbscanClusterExecutions(successfulExecutions) // TODO: only successful?
            } else emptyMap()

        if (clustered.size > 1) {
            for (c in clustered) {
                clusters +=
                    SuccessfulExecutionCluster(
                        "$commentPrefix ${ExecutionGroup.SUCCESSFUL_EXECUTIONS.displayName} #${clustered.keys.indexOf(c.key)} $commentPostfix",
                        c.value.toList()
                    )
            }
        } else {
            clusters +=
                SuccessfulExecutionCluster(
                    "$commentPrefix ${ExecutionGroup.SUCCESSFUL_EXECUTIONS.displayName} $commentPostfix",
                    successfulExecutions.toList()
                )
        }
    }

    clusters += addClustersOfFailedExecutions(grouped, commentPrefix, commentPostfix)
    return clusters
}

private fun addClustersOfFailedExecutions(
    grouped: Map<ExecutionGroup, List<UtExecution>>,
    commentPrefix: String,
    commentPostfix: String
): List<FailedExecutionCluster> {
    val clusters = grouped
        .filterNot { (kind, _) -> kind == ExecutionGroup.SUCCESSFUL_EXECUTIONS }
        .map { (suffixId, group) ->
            FailedExecutionCluster("$commentPrefix ${suffixId.displayName} $commentPostfix", group)
        }

    return clusters
}

/** The group of execution to be presented in the generated source file with tests. */
enum class ExecutionGroup {
    SUCCESSFUL_EXECUTIONS,
    ERROR_SUITE,
    CHECKED_EXCEPTIONS,
    EXPLICITLY_THROWN_UNCHECKED_EXCEPTIONS,
    OVERFLOWS,
    TIMEOUTS,
    CRASH_SUITE,
    TAINT_ANALYSIS,
    SECURITY;

    val displayName: String get() = toString().replace('_', ' ')
}

private fun UtExecutionResult.clusterKind() = when (this) {
    is UtExecutionSuccess -> ExecutionGroup.SUCCESSFUL_EXECUTIONS
    is UtImplicitlyThrownException -> if (this.exception.isCheckedException) ExecutionGroup.CHECKED_EXCEPTIONS else ExecutionGroup.ERROR_SUITE
    is UtExplicitlyThrownException -> if (this.exception.isCheckedException) ExecutionGroup.CHECKED_EXCEPTIONS else ExecutionGroup.EXPLICITLY_THROWN_UNCHECKED_EXCEPTIONS
    is UtStreamConsumingFailure -> ExecutionGroup.ERROR_SUITE
    is UtOverflowFailure -> ExecutionGroup.OVERFLOWS
    is UtTimeoutException -> ExecutionGroup.TIMEOUTS
    is UtConcreteExecutionFailure -> ExecutionGroup.CRASH_SUITE
    is UtSandboxFailure -> ExecutionGroup.SECURITY
    is UtTaintAnalysisFailure -> ExecutionGroup.TAINT_ANALYSIS
}

/**
 * Structure used to represent execution cluster with header
 */
sealed class ExecutionCluster(var header: String, val executions: List<UtExecution>)

/**
 * Represents successful execution cluster
 */
private class SuccessfulExecutionCluster(header: String, executions: List<UtExecution>) :
    ExecutionCluster(header, executions)

/**
 * Represents failed execution cluster
 */
private class FailedExecutionCluster(header: String, executions: List<UtExecution>) :
    ExecutionCluster(header, executions)

/**
 * Removes intersections (steps that occur in all of successful executions) from cluster comment
 * If false then intersections will be printed in cluster comment
 */
private const val REMOVE_INTERSECTIONS: Boolean = true

/**
 * Represents execution cluster
 * Contains the entities required for summarization
 */
data class TraceTagCluster(
    var clusterHeader: String,
    val traceTags: List<TraceTag>,
    val commonStepsTraceTag: TraceTagWithoutExecution,
    val isSuccessful: Boolean
)
