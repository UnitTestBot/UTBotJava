package org.utbot.engine

import org.utbot.engine.state.ExecutionState

/**
 * [UtBotSymbolicEngine] will fire an event every time it traverses new [ExecutionState].
 */
fun interface ExecutionStateListener {
    fun visit(graph: InterProceduralUnitGraph, state: ExecutionState)
}