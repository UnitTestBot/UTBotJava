package org.utbot.features

import org.utbot.analytics.FeatureExtractor
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.strategies.StatementsStatistics
import org.utbot.engine.selectors.strategies.SubpathStatistics

/**
 * Implementation of feature extractor.
 * Extract features for state and stores it in features vector of this state.
 */
class FeatureExtractorImpl(private val graph: InterProceduralUnitGraph) : FeatureExtractor {
    companion object {
        private val subpathGuidedSelectorIndexes = listOf(0, 1, 2, 3)
    }

    private fun <T : Number> MutableList<Double>.add(value: T) = add(value.toDouble())
    private fun <T : Collection<*>> MutableList<Double>.add(value: T) = add(value.size)

    private val subpathStatistics = subpathGuidedSelectorIndexes.map { SubpathStatistics(graph, it) }
    private val statementStatistics = StatementsStatistics(graph)

    override fun extractFeatures(executionState: ExecutionState, generatedTestCases: Int) {
        if (executionState.features.isNotEmpty()) {
            executionState.features.clear()
        }

        executionState.features.add(executionState.executionStack) // stack
        executionState.features.add(graph.succs(executionState.stmt)) // successor
        executionState.features.add(generatedTestCases) // testCase
        executionState.features.add(executionState.visitedAfterLastFork) // coverage by branch
        executionState.features.add(executionState.visitedBeforeLastFork + executionState.visitedAfterLastFork) // coverage by path
        executionState.features.add(executionState.depth) // depth
        executionState.features.add(statementStatistics.statementInMethodCount(executionState)) // cpicnt
        executionState.features.add(statementStatistics.statementCount(executionState)) // icnt
        executionState.features.add(executionState.stmtsSinceLastCovered) // covNew
        subpathStatistics.forEach {
            executionState.features.add(it.subpathCount(executionState)) // sgs_i
        }
    }
}