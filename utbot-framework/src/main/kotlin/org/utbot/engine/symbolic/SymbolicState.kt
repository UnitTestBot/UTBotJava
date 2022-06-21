package org.utbot.engine.symbolic

import org.utbot.engine.Memory
import org.utbot.engine.MemoryUpdate
import org.utbot.engine.pc.UtSolver

/**
 * Represents an immutable symbolic state.
 *
 * @param [solver] stores [UtSolver] with symbolic constraints.
 * @param [memory] stores the current state of the symbolic memory.
 */
data class SymbolicState(
    val solver: UtSolver,
    val memory: Memory = Memory(),
) {
    operator fun plus(update: SymbolicStateUpdate): SymbolicState =
        with(update) {
            SymbolicState(
                solver.add(hard = hardConstraints, soft = softConstraints, assumption = assumptions),
                memory = memory.update(memoryUpdates),
            )
        }

    operator fun plus(update: HardConstraint): SymbolicState =
        plus(SymbolicStateUpdate(hardConstraints = update))

    operator fun plus(update: SoftConstraint): SymbolicState =
        plus(SymbolicStateUpdate(softConstraints = update))

    operator fun plus(update: MemoryUpdate): SymbolicState =
        plus(SymbolicStateUpdate(memoryUpdates = update))


    fun stateForNestedMethod() = copy(
        memory = memory.memoryForNestedMethod()
    )

}