package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.BasePathSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.selectors.strategies.StrategyObserver
import kotlin.properties.Delegates
import kotlin.random.Random

/**
 * Selects ExecutionState with maximum weight.
 * If there are several states with equal maximum weight, than selects random from them.
 */
abstract class GreedySearch(
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int = 42
) : BasePathSelector(choosingStrategy, stoppingStrategy), StrategyObserver {
    private val states = mutableSetOf<ExecutionState>()

    private val randomGen: Random = Random(seed)

    override fun update() {
        // do nothing by default
    }

    override fun offerImpl(state: ExecutionState) {
        states += state
    }

    override fun pollImpl(): ExecutionState? = peekImpl()?.also { remove(it) }

    /**
     * Creates set of states with maximum weight and then pick random
     */
    override fun peekImpl(): ExecutionState? {
        if (isEmpty()) {
            return null
        }

        val candidates = mutableListOf<ExecutionState>()
        var bestWeight by Delegates.notNull<Double>()

        states.forEach {
            val weight = it.weight

            if (candidates.isEmpty()) {
                bestWeight = weight
                candidates += it
            } else {
                if (bestWeight <= weight) {
                    if (bestWeight < weight) {
                        bestWeight = weight
                        candidates.clear()
                    }
                    candidates += it
                }
            }
        }

        return candidates.random(randomGen)
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        return states.remove(state)
    }

    override fun isEmpty() = states.isEmpty()

    override fun close() {
        states.forEach {
            it.close()
        }
    }

    protected abstract val ExecutionState.weight: Double
}