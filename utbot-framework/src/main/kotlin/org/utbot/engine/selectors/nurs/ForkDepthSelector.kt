package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Execution states are weighted as 1.0 / (exState.depth + 1),
 * where exState.depth is number of forks on the executionState's path
 *
 * @see [NonUniformRandomSearch]
 */
class ForkDepthSelector(
    policy: ChoosingStrategy,
    stoppingPolicy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(policy, stoppingPolicy, seed) {

    override val ExecutionState.cost: Double
        get() = depth.toDouble()

    override val name: String
        get() = "ForkDepth"
}
