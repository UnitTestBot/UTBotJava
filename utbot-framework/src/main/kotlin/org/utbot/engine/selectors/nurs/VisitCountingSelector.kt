package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.EdgeVisitCountingStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * PathSelector that uses [EdgeVisitCountingStatistics] and
 * gives more weight to paths that have been visited less times
 */
class VisitCountingSelector(
    override val choosingStrategy: EdgeVisitCountingStatistics,
    stoppingStrategy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {
    init {
        choosingStrategy.subscribe(this)
    }

    override val name: String
        get() = "VisitCountingSelector"

    override val ExecutionState.cost: Double
        get() = (lastEdge?.let { choosingStrategy.visitCounter[it] } ?: 0) + 1.0
}