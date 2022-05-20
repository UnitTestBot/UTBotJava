package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.DistanceStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Execution states are weighted as 1.0 / max(closestToUncovered[exState.stmt], 1.0)
 *
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L218
 *
 * According to assumption that closest to uncovered statements
 * execution states that are more likely to reach new coverage
 * successfully. Using this strategy, we will select these execution
 * states with higher probability.
 *
 * @see NonUniformRandomSearch
 */
class MinimalDistanceToUncovered(
    override val choosingStrategy: DistanceStatistics,
    stoppingPolicy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingPolicy, seed) {
    init {
        choosingStrategy.subscribe(this)
    }

    override val ExecutionState.cost: Double
        get() = choosingStrategy.distanceToUncovered(this).let { d ->
            if (d == Int.MAX_VALUE) {
                Double.MAX_VALUE
            } else {
                (d + pathLength).toDouble() / (edges.size + 1)
            }
        }

    override val name = "NURS:MD2U"
}