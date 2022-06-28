package org.utbot.engine.selectors

import org.utbot.engine.*
import org.utbot.engine.selectors.nurs.NonUniformRandomSearch
import org.utbot.engine.selectors.strategies.ScoringStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy

class ScoringPathSelector(
    override val choosingStrategy: ScoringStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {

    init {
        choosingStrategy.subscribe(this)
    }

    override val ExecutionState.cost: Double
        get() = choosingStrategy[this]

    override val name = "ScoringPathSelector"
}