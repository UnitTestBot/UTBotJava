package org.utbot.engine.selectors.strategies

import org.utbot.engine.state.Edge
import org.utbot.engine.state.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.pathLogger

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
    var exceedingStepsLimit: Boolean = false
        private set

    override fun shouldStop(): Boolean {
        val shouldDrop = stepsCounter > stepsLimit

        if (shouldDrop) {
            exceedingStepsLimit = true
            pathLogger.debug { "Steps limit has been exceeded: current step limit is $stepsLimit" }
        }

        return shouldDrop
    }

    override fun onVisit(edge: Edge) {
        stepsCounter++
    }

    override fun onTraversed(executionState: ExecutionState) {
        stepsCounter = 0
    }
}