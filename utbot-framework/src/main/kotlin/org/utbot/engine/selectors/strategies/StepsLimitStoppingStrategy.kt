package org.utbot.engine.selectors.strategies

import org.utbot.engine.Edge
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph

/**
 * Stopping strategy that suggest to stop execution
 * after stepsLimit visiting new edges without successful
 * complete traverse of executionState
 */
class StepsLimitStoppingStrategy(
    private val stepsLimit: Int,
    graph: InterProceduralUnitGraph
) : TraverseGraphStatistics(graph), StoppingStrategy {
    private var stepsCounter: Int = 0

    override fun shouldStop(): Boolean {
        return stepsCounter > stepsLimit
    }

    override fun onVisit(edge: Edge) {
        stepsCounter++
    }

    override fun onTraversed(executionState: ExecutionState) {
        stepsCounter = 0
    }
}