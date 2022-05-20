package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import kotlin.random.Random

/**
 * Selects each time a random state from the queue.
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L137
 *
 * @see BasePathSelector
 * @see PathSelector
 */
class RandomSelector(choosingStrategy: ChoosingStrategy, stoppingStrategy: StoppingStrategy, seed: Int = 42) :
    BasePathSelector(choosingStrategy, stoppingStrategy) {
    private val executionStates = mutableListOf<ExecutionState>()
    private val random = Random(seed)
    private var currentIndex = -1

    override fun offerImpl(state: ExecutionState) {
        executionStates += state
        currentIndex = -1
    }

    override fun peekImpl(): ExecutionState? {
        if (executionStates.size == 0) {
            return null
        }
        if (currentIndex == -1) {
            currentIndex = random.nextInt(executionStates.size)
        }
        return executionStates[currentIndex]
    }

    override fun pollImpl(): ExecutionState? {
        if (executionStates.size == 0) {
            return null
        }
        if (currentIndex == -1) {
            currentIndex = random.nextInt(executionStates.size)
        }
        val state = executionStates[currentIndex]
        executionStates.removeAt(currentIndex)
        currentIndex = -1
        return state
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        currentIndex = -1
        return executionStates.remove(state)
    }

    override fun close() {
        executionStates.forEach {
            it.close()
        }
    }

    override fun isEmpty() =
        executionStates.isEmpty()

    override val name = "RandomSelector"
}