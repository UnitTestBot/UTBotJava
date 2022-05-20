package org.utbot.engine.selectors.strategies

import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph

/**
 * Statistics that suggest which execution states can be dropped
 */
interface ChoosingStrategy {
    val graph: InterProceduralUnitGraph

    fun shouldDrop(state: ExecutionState): Boolean
}