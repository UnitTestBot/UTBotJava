package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState

/**
 * Retrieves states from different pathSelectors in rotation.
 *
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L511
 *
 * For example InterleavedSelector(arrayOf(DFSSelector, BFSSelector))
 * will keep 2 queues for different kind of searches,
 * and it will choose state from DFSSelector on each odd step,
 * and from BFSSelector on each even step.
 *
 * @see PathSelector
 */
class InterleavedSelector(val selectors: List<PathSelector>) : PathSelector {
    var index = 0

    override fun offer(state: ExecutionState) {
        selectors.forEach {
            it.offer(state)
        }
    }

    override fun poll(): ExecutionState? {
        val res = peek() ?: return null
        this.remove(res)
        return res
    }

    override fun peek(): ExecutionState? {
        return selectors[index].peek()
    }

    override fun remove(state: ExecutionState): Boolean {
        index = (index + 1) % selectors.size
        return selectors.all {
            it.remove(state)
        }
    }

    override fun close() {
        selectors.forEach {
            it.close()
        }
    }

    override fun queue(): List<Pair<ExecutionState, Double>> {
        return emptyList()
    }

    override fun isEmpty(): Boolean = selectors.all { it.isEmpty() }

    override val name = "InterleavedSelector : ${selectors.map { it.name }}"
}