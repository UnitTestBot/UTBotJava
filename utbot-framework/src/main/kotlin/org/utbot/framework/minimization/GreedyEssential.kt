package org.utbot.framework.minimization

import java.util.PriorityQueue

private inline class ExecutionNumber(val number: Int)

private inline class LineNumber(val number: Int)

/**
 * [Greedy essential algorithm](CONFLUENCE:Test+Minimization)
 */
class GreedyEssential private constructor(
    executionToCoveredLines: Map<ExecutionNumber, List<LineNumber>>
) {
    private val executionToUsefulLines: Map<ExecutionNumber, MutableSet<LineNumber>> =
        executionToCoveredLines
            .mapValues { it.value.toMutableSet() }

    private val lineToUnusedCoveringExecutions: Map<LineNumber, MutableSet<ExecutionNumber>> =
        executionToCoveredLines
            .flatMap { (execution, lines) -> lines.map { it to execution } }
            .groupBy({ it.first }) { it.second }
            .mapValues { it.value.toMutableSet() }

    private val executionByPriority =
        PriorityQueue(compareByDescending<Pair<ExecutionNumber, Int>> { it.second }.thenBy { it.first.number })
            .apply {
                addAll(
                    executionToCoveredLines
                        .keys
                        .map { it to executionToUsefulLines[it]!!.size }
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
        execution to executionToUsefulLines[execution]!!.size

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
         * @return retained execution ids.
         */
        fun minimize(executions: Map<Int, List<Int>>): List<Int> {
            val convertedExecutions = executions
                .entries
                .associate { (execution, lines) -> ExecutionNumber(execution) to lines.map { LineNumber(it) } }

            val prioritizer = GreedyEssential(convertedExecutions)
            val list = mutableListOf<ExecutionNumber>()
            while (prioritizer.hasMore()) {
                list.add(prioritizer.getExecutionAndRemove())
            }

            return list.map { it.number }
        }
    }
}

