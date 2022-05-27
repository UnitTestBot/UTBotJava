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
        with(executionState.features) {
            if (isNotEmpty()) {
                clear()
            }

            add(executionState.executionStack) // stack
            add(graph.succs(executionState.stmt)) // successor
            add(generatedTestCases) // testCase
            add(executionState.visitedAfterLastFork) // coverage by branch
            add(executionState.visitedBeforeLastFork + executionState.visitedAfterLastFork) // coverage by path
            add(executionState.depth) // depth
            add(statementStatistics.statementInMethodCount(executionState)) // cpicnt
            add(statementStatistics.statementCount(executionState)) // icnt
            add(executionState.stmtsSinceLastCovered) // covNew

            subpathStatistics.forEach {
                add(it.subpathCount(executionState)) // sgs_i
            }
        }
    }
}