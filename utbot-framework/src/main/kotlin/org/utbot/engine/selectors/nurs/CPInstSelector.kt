package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StatementsStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Execution states are weighted as 1.0 / StatementStatistics.statementsInMethodCount(exState),
 * where StatementStatistics.statementsInMethodCount(exState)
 * is number of visited instructions in the current function of execution state
 *
 * @see @see <a href=https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L207>Klee analog</a>
 *
 * [NonUniformRandomSearch]
 */
class CPInstSelector(
    private val statementStatistics: StatementsStatistics,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {

    override val name: String
        get() = "NURS:CPICnt"

    override val ExecutionState.cost: Double
        get() = statementStatistics.statementInMethodCount(this).toDouble()
}
