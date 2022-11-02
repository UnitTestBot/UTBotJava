package org.utbot.engine.selectors.random

import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.state.ExecutionState
import org.utbot.framework.UtSettings

/**
 * This selector chooses states like [RandomSelector] does, but drops states from big iteration loops
 * (based on [UtSettings.loopStepsLimit]).
 */
class RandomSelectorWithLoopIterationsThreshold(
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int = 42
) : RandomSelector(choosingStrategy, stoppingStrategy, seed) {
    override fun offerImpl(state: ExecutionState) {
        with(state) {
            val numberOfStmtOccurrencesInPath = visitedStatementsHashesToCountInPath[stmt.hashCode()] ?: 0

            // Drop loop state if it exceeds loop steps limit
            UtSettings.loopStepsLimit
                .takeIf { it > 0 }
                ?.let {
                    if (numberOfStmtOccurrencesInPath > it) {
                        return
                    }
                }
        }

        executionStates += state
        currentIndex = -1
    }

    override val name: String = this::class.java.simpleName
}
