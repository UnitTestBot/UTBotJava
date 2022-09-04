package org.utbot.framework.minimization

import java.util.PriorityQueue

@JvmInline
private value class ExecutionNumber(val number: Int)

@JvmInline
private value class LineNumber(val number: Int)

/**
 * Execution source priority (symbolic executions are considered more important than fuzzed executions).
 * @see [UtSettings.preferSymbolicExecutionsDuringMinimization]
 */
@JvmInline
private value class SourcePriority(val number: Int)

/**
 * A wrapper that combines covered lines with the execution source priority
 */
private data class ExecutionCoverageInfo(
    val sourcePriority: SourcePriority,
    val coveredLines: List<LineNumber>
)

/**
 * [Greedy essential algorithm](CONFLUENCE:Test+Minimization)
 */
class GreedyEssential private constructor(
    executionToCoveredLines: Map<ExecutionNumber, ExecutionCoverageInfo>
) {
    private val executionToSourcePriority: Map<ExecutionNumber, SourcePriority> =
        executionToCoveredLines
            .mapValues { it.value.sourcePriority }

    private val executionToUsefulLines: Map<ExecutionNumber, MutableSet<LineNumber>> =
        executionToCoveredLines
            .mapValues { it.value.coveredLines.toMutableSet() }

    private val lineToUnusedCoveringExecutions: Map<LineNumber, MutableSet<ExecutionNumber>> =
        executionToCoveredLines
            .flatMap { (execution, lines) -> lines.coveredLines.map { it to execution } }
            .groupBy({ it.first }) { it.second }
            .mapValues { it.value.toMutableSet() }

    private val executionByPriority =
        PriorityQueue(
            compareByDescending<Triple<ExecutionNumber, Int, SourcePriority>> { it.second }
                    .thenBy { it.third.number }
                    .thenBy { it.first.number }
        )
            .apply {
                addAll(
                    executionToCoveredLines
                        .keys
                        .map { Triple(it, executionToUsefulLines[it]!!.size, executionToSourcePriority[it]!!) }
                )
            }

    private val essentialExecutions: MutableList<ExecutionNumber> =
        lineToUnusedCoveringExecutions
            .filter { (_, executions) -> executions.size == 1 }
            .values
            .flatten()
            .distinct()
            .toMutableList()

    private fun removeExecution(execution: ExecutionNumber) {
        val newlyCoveredLines = executionToUsefulLines[execution]!!.toMutableSet()
        if (newlyCoveredLines.isEmpty()) {
            executionByPriority.remove(executionToPriority(execution))
            return
        }
        for (line in newlyCoveredLines) {
            val unusedCoveringExecutions = lineToUnusedCoveringExecutions[line]!!.toMutableList()
            for (coveringExecution in unusedCoveringExecutions) {
                removeLineFromExecution(coveringExecution, line)
            }
        }
    }

    private fun hasMore() = executionByPriority.isNotEmpty()

    private fun getExecutionAndRemove(): ExecutionNumber {

        val bestExecution = if (essentialExecutions.isNotEmpty()) {
            essentialExecutions.removeLast()
        } else {
            executionByPriority.peek()?.first
                ?: error("No new executions could be added. Everything is already covered.")
        }
        removeExecution(bestExecution)
        return bestExecution
    }

    private fun executionToPriority(execution: ExecutionNumber) =
        Triple(execution, executionToUsefulLines[execution]!!.size, executionToSourcePriority[execution]!!)

    private fun removeLineFromExecution(execution: ExecutionNumber, line: LineNumber) {
        executionByPriority.remove(executionToPriority(execution))

        executionToUsefulLines[execution]!!.remove(line)
        lineToUnusedCoveringExecutions[line]!!.remove(execution)

        if (executionToUsefulLines[execution]!!.isNotEmpty()) {
            executionByPriority.add(executionToPriority(execution))
        }
    }

    companion object {
        /**
         * Minimizes the given [executions] assuming the map represents mapping from execution id to covered
         * instruction ids.
         *
         * @param executions the mapping of execution ids to lists of lines covered by the execution.
         * @param sourcePriorities execution priorities: lower values correspond to more important executions
         *        that should be kept everything else being equal.
         *
         * @return retained execution ids.
         */
        fun minimize(executions: Map<Int, List<Int>>, sourcePriorities: Map<Int, Int> = emptyMap()): List<Int> {
            val convertedExecutions = executions
                .entries
                .associate { (execution, lines) ->
                    ExecutionNumber(execution) to ExecutionCoverageInfo(
                        SourcePriority(sourcePriorities.getOrDefault(execution, 0)),
                        lines.map { LineNumber(it) }
                    )
                }

            val prioritizer = GreedyEssential(convertedExecutions)
            val list = mutableListOf<ExecutionNumber>()
            while (prioritizer.hasMore()) {
                list.add(prioritizer.getExecutionAndRemove())
            }

            return list.map { it.number }
        }
    }
}

