package org.utbot.engine.selectors

import org.utbot.engine.*
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.ScoringStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import java.util.PriorityQueue

open class ScoringPathSelector(
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    protected val scoringStrategy: ScoringStrategy,
) : BasePathSelector(choosingStrategy, stoppingStrategy) {
    private val states = PriorityQueue<ExecutionState> { a, b ->
        val aScore = scoringStrategy.score(a)
        val bScore = scoringStrategy.score(b)
        aScore.compareTo(bScore)
    }

    override fun offerImpl(state: ExecutionState) {
        scoringStrategy.score(state)
        states.add(state)
    }

    override fun pollImpl(): ExecutionState? {
        return states.poll()
    }

    override fun peekImpl(): ExecutionState? {
        return states.firstOrNull()
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        return states.remove(state)
    }

    override fun isEmpty() = states.isEmpty()

    override val name = "ScoringSelector"

    override fun close() {
        states.forEach {
            it.close()
        }
    }
}