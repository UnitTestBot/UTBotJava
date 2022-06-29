package org.utbot.engine

/**
 * Represents a mutable _Context_ during the [ExecutionState] traversing. This _Context_ consists of all mutable and
 * immutable properties and fields which are created and updated during analysis of a **single** Jimple instruction.
 *
 * Traverser functions should be implemented as an extension functions with [TraversalContext] as a receiver.
 *
 * TODO: extend this class with other properties, such as [Environment], which is [Traverser] mutable property now.
 */
class TraversalContext {
    // TODO: move properties from [UtBotSymbolicEngine] here


    // TODO: Q: maybe it's better to pass stateConsumer as an argument to constructor?
    private val states = mutableListOf<ExecutionState>()

    /**
     * Offers new [ExecutionState] which can be obtained later with [nextStates].
     */
    fun offerState(state: ExecutionState) {
        states.add(state)
    }

    /**
     * New states obtained from the traversal.
     */
    val nextStates: Collection<ExecutionState>
        get() = states
}