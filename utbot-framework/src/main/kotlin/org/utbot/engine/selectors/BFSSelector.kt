package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy


/**
 * PathSelector that traverses ExecutionStates with BFS strategy.
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L89
 *
 * @see BasePathSelector
 * @see PathSelector
 */
class BFSSelector(choosingStrategy: ChoosingStrategy, stoppingStrategy: StoppingStrategy) :
    BasePathSelector(choosingStrategy, stoppingStrategy) {
    private val states = ArrayDeque<ExecutionState>()

    override fun offerImpl(state: ExecutionState) {
        states.addLast(state)
    }

    override fun pollImpl(): ExecutionState? {
        return states.removeFirstOrNull()
    }

    override fun peekImpl(): ExecutionState? {
        return states.firstOrNull()
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        return states.remove(state)
    }

    override fun isEmpty() = states.isEmpty()

    override val name = "BFSSelector"

    override fun close() {
        states.forEach {
            it.close()
        }
    }
}