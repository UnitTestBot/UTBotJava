package org.utbot.engine.selectors.strategies

import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph

class GeneratedTestCountingStatistics(
    graph: InterProceduralUnitGraph
) : TraverseGraphStatistics(graph) {
    var generatedTestsCount = 0
        private set

    override fun onTraversed(executionState: ExecutionState) {
        generatedTestsCount++
    }
}