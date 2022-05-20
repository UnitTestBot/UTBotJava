package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.DistanceStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Execution states are weighted as follows:
 * val lenMetric = (exState.pathLength / (exState.edges.size + 1))
 * if (exState.edges contains uncovered stmt) then
 *      1.0 / (lenMetric + 1)
 * else
 *      1.0 / (closestToUncovered[exState.stmt] + lenMetric + 1)
 *
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L217
 *
 * By using this selector we select execution states according to assumption that
 * execution states that are closest to uncovered statements are more likely
 * to reach new coverage and also taking into the account currently uncovered
 * statements that occurs in path.
 *
 *  @see NonUniformRandomSearch
 *  @see org.utbot.engine.selectors.PathSelector
 */
class CoveredNewSelector(
    override val choosingStrategy: DistanceStatistics,
    stoppingStrategy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {
    init {
        choosingStrategy.subscribe(this)
    }

    override val ExecutionState.cost: Double
        get() = choosingStrategy.distanceToUncovered(this).let { d ->
            when {
                edges.any { !choosingStrategy.isCovered(it) } -> pathLength.toDouble() / (edges.size + 1)
                d == Int.MAX_VALUE -> Double.MAX_VALUE
                else -> d + pathLength.toDouble() / (edges.size + 1)
            }
        }

    override val name = "NURS:CovNew"
}