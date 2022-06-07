package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import org.utbot.engine.isPreconditionCheckMethod
import org.utbot.engine.pathLogger
import org.utbot.engine.pc.UtSolver
import org.utbot.engine.pc.UtSolverStatusKind.SAT
import org.utbot.engine.pc.UtSolverStatusUNSAT
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.framework.UtSettings


/**
 * The base PathSelector class.
 *
 * Maintains the common logic of:
 * - adding new ExecutionState with dropping it according to choosingStrategy
 * - stopping execution according to stoppingStrategy
 * - fast unsat on forks while polling next executionState
 *
 * @see PathSelector
 */

abstract class BasePathSelector(
    protected open val choosingStrategy: ChoosingStrategy,
    protected val stoppingStrategy: StoppingStrategy
) : PathSelector {
    override val remainingStatesForConcreteExecution: List<ExecutionState>
        // return copy and clear the original
        get() = statesForConcreteExecution.toList().also { statesForConcreteExecution.clear() }

    protected var current: ExecutionState? = null

    /**
     * All remaining states after stopping by [stoppingStrategy] are saved here
     * in case [UtSettings.saveRemainingStatesForConcreteExecution] is enabled.
     */
    private val statesForConcreteExecution: MutableList<ExecutionState> = mutableListOf()

    override fun offer(state: ExecutionState) {
        if (state.solver.lastStatus is UtSolverStatusUNSAT || choosingStrategy.shouldDrop(state)) {
            state.close()
            return
        }

        current = null
        offerImpl(state)
    }


    override fun peek(): ExecutionState? {
        if (current == null) {
            current = peekImpl()
        }
        return current
    }


    /**
     * @return true if [utSolver] constraints are satisfiable
     */
    private fun checkUnsat(utSolver: UtSolver): Boolean =
        utSolver.assertions.isNotEmpty() && utSolver.check(respectSoft = false).statusKind != SAT

    /**
     * check fast unsat on forks
     */
    private fun checkUnsatIfFork(state: ExecutionState) =
        state.path.isNotEmpty() && choosingStrategy.graph.isFork(state.path.last()) && checkUnsat(state.solver)

    override fun poll(): ExecutionState? {
        if (stoppingStrategy.shouldStop()) {
            saveRemainingStatesForConcreteExecution()
            return null
        }

        while (!isEmpty()) {
            val state = pollImpl()!!
            pathLogger.trace { "poll next state (lastStatus=${state.solver.lastStatus}): " + state.prettifiedPathLog() }

            current = null
            if (choosingStrategy.shouldDrop(state) || checkUnsatIfFork(state)) {
                state.close()
                continue
            }

            // If we have failed assumes, we try to execute the state concretely
            if (state.solver.failedAssumptions.isNotEmpty()) {
                // But we do not want to execute concretely states because of
                // controversies during `preconditionCheck` analysis
                if (state.lastMethod?.isPreconditionCheckMethod == true) {
                    state.close()
                } else {
                    statesForConcreteExecution += state
                }
                continue
            }

            return state
        }
        return null
    }

    override fun remove(state: ExecutionState): Boolean {
        current = null
        return removeImpl(state)
    }

    override fun queue(): List<Pair<ExecutionState, Double>> {
        return emptyList()
    }

    /**
     * Implementation of removing exactly one executionState
     *
     * @see BasePathSelector.remove
     * @see PathSelector.remove
     */
    protected abstract fun removeImpl(state: ExecutionState): Boolean

    /**
     * Implementation of removing first executionState in queue
     * @see BasePathSelector.poll
     * @see PathSelector.poll
     */
    protected abstract fun pollImpl(): ExecutionState?

    /**
     * Implementation of peeking an executionState
     *
     * @see BasePathSelector.peek
     * @see PathSelector.peek
     */
    protected abstract fun peekImpl(): ExecutionState?

    /**
     * Implementation of offering new ExecutionState to queue
     * @see BasePathSelector.offer
     * @see PathSelector.offer
     */
    protected abstract fun offerImpl(state: ExecutionState)

    /**
     * Moves all remaining states from states queue to [statesForConcreteExecution]
     * if [UtSettings.saveRemainingStatesForConcreteExecution] is enabled.
     */
    private fun saveRemainingStatesForConcreteExecution() {
        if (UtSettings.saveRemainingStatesForConcreteExecution) {
            while (!isEmpty()) {
                val state = peek()!!
                statesForConcreteExecution += state
                remove(state)
            }
        }
    }
}
