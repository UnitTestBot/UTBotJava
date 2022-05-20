package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import kotlin.math.pow

/**
 * Execution states are weighted as 0.5 ^ exState.pathLength.
 *
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L199
 *
 * Due to this, execution states with longer path have much lower
 * probability to be picked in comparison with DepthSelector
 *
 * @see NonUniformRandomSearch
 */
class RPSelector(
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {

    override val ExecutionState.cost: Double
        get() = 2.0.pow(pathLength)

    override val name = "NURS:RP"
}