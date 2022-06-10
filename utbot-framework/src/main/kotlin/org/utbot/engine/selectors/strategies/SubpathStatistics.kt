package org.utbot.engine.selectors.strategies

import org.utbot.engine.CALL_DECISION_NUM
import org.utbot.engine.Edge
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.framework.UtSettings
import kotlin.math.pow

/**
 * Calculates frequency of subpathes of statements with length equal to 2^{index}.
 */
class SubpathStatistics(
    graph: InterProceduralUnitGraph,
    index: Int = UtSettings.subpathGuidedSelectorIndex
) : TraverseGraphStatistics(graph) {
    private val subpathCount = mutableMapOf<List<Edge>, Int>()
    private val length: Int = 2.0.pow(index).toInt()

    /**
     * Take length last edges from state's path and handle exception edges
     */
    private fun ExecutionState.getSubpath(length: Int): List<Edge> {
        val subpath = mutableListOf<Edge>()
        val actualLength = if (pathLength >= length) length else pathLength

        var current = stmt
        var exceptionNumber = 0
        (0 until actualLength).forEach {
            val decision = decisionPath[decisionPath.size - it - 1]
            if (decision < CALL_DECISION_NUM) {
                exceptionNumber++
            }
            val i = path.size - it - 1 + exceptionNumber
            val prev = if (i == path.size) stmt else path[i]
            subpath += Edge(prev, current, decision)
            current = prev
        }

        return subpath
    }

    override fun onVisit(executionState: ExecutionState) {
        subpathCount.compute(executionState.getSubpath(length)) { _, v ->
            v?.plus(1) ?: 1
        }
    }

    fun subpathCount(executionState: ExecutionState): Int =
        subpathCount.getOrPut(executionState.getSubpath(length)) { 1 }
}