package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.selectors.strategies.SubpathStatistics

/**
 * Peeks state with minimum frequency of relative subpath.
 * See https://github.com/eth-sri/learch/blob/master/klee/lib/Core/Searcher.cpp#L738
 *
 * @see [GreedySearch]
 */
class SubpathGuidedSelector(
    private val subpathStatistics: SubpathStatistics,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int = 42
) : GreedySearch(choosingStrategy, stoppingStrategy, seed) {


    override val name
        get() = "NURS:SubpathGuidedSearch"

    /**
     * Use - to find minimum
     */
    override val ExecutionState.weight: Double
        get() = -subpathStatistics.subpathCount(this).toDouble()
}