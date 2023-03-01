package org.utbot.engine

import org.utbot.engine.state.ExecutionState

interface StateListener {
    fun visit(graph: InterProceduralUnitGraph, state: ExecutionState)
}