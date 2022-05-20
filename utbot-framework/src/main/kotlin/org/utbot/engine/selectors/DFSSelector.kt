package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy


/**
 * PathSelector that traverses ExecutionStates with BFS strategy.
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L53
 *
 * @see BasePathSelector
 * @see PathSelector
 */
class DFSSelector(choosingStrategy: ChoosingStrategy, stoppingStrategy: StoppingStrategy) :
    BasePathSelector(choosingStrategy, stoppingStrategy) {
    private val states = mutableListOf<ExecutionState>()

    override fun offerImpl(state: ExecutionState) {
        states += state
    }

    override fun peekImpl(): ExecutionState? {
        return states.lastOrNull()
    }

    override fun pollImpl(): ExecutionState? {
        return states.removeLastOrNull()
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        return states.remove(state)
    }

    override fun close() {
        states.forEach {
            it.close()
        }
    }

    override fun isEmpty() =
        states.isEmpty()

    override val name = "DFSSelector"
}
