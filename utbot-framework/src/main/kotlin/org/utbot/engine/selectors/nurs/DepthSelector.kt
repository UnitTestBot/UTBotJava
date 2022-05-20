package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Execution states are weighted as 1.0 / (exState.pathLength + 1).
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L197
 *
 * Execution states with shorter path length are more likely to be picked.
 *
 * @see NonUniformRandomSearch
 */
class DepthSelector(
    policy: ChoosingStrategy,
    stoppingPolicy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(policy, stoppingPolicy, seed) {

    override val ExecutionState.cost: Double
        get() = pathLength.toDouble()

    override val name = "NURS:Depth"
}