package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import org.utbot.engine.pc.UtSolverStatusKind

/**
 * Class container for ExecutionStates.
 *
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.h#L38
 */
interface PathSelector : AutoCloseable {

    /**
     * Saved remaining states to execute them concretely later.
     */
    val remainingStatesForConcreteExecution: List<ExecutionState>
        get() = emptyList()

    /**
     * Adds ExecutionState to PathSelector.
     *
     * The executionState can be not added to PathSelector,
     * as it can be already traversed or cannot reach new coverage.
     */
    fun offer(state: ExecutionState)

    /**
     * Removes ExecutionState from PathSelector.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present.
     */
    fun remove(state: ExecutionState): Boolean

    /**
     * Gets next executionState from PathSelector without removing
     * or `null` if it is empty.
     *
     * Each call of peek() returns the same value while
     * PathSelector isn't changed.
     */
    fun peek(): ExecutionState?

    /**
     * Gets and removes next executionState from PathSelector
     * or return null if
     * * There is no more executionStates inside PathSelector.
     * * Stopping strategy suggest to stop execution.
     * * Choosing strategy suggest to drop next executionState
     *
     * Should behave as follow:
     * poll() {
     *     executionState = peek()
     *     this -= executionState
     *     return executionState
     * }
     */
    fun poll(): ExecutionState?

    /**
     * Get current queue in selector
     */
    fun queue(): List<Pair<ExecutionState, Double>>

    fun isEmpty(): Boolean

    /**
     * Debug name of PathSelector
     */
    val name: String
}

/**
 * Drops all states from the queue whose status is UNSAT or UNKNOWN until the first SAT state is met.
 * Thus, it doesn't make any solver checks.
 *
 * @return the first SAT state or `null` if there is no such.
 */
fun PathSelector.pollUntilFastSAT(): ExecutionState? {
    while (true) {
        val state = peek() ?: return null
        remove(state)
        if (state.solver.lastStatus.statusKind == UtSolverStatusKind.SAT) {
            return state
        }
    }
}