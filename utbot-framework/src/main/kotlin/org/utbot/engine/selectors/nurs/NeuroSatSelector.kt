package org.utbot.engine.selectors.nurs

import org.utbot.analytics.Predictors
import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * PathSelector that gives weight to executionState according to NeuroSat predictions
 */
class NeuroSatSelector(
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {

    override val ExecutionState.cost: Double
        get() = Predictors.smt.predict(solver.assertions).toDouble()

    override val name: String
        get() = "NeuroSatSelector"
}