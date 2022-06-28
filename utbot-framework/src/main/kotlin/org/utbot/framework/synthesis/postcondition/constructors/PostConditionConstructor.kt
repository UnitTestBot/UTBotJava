package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.SymbolicResult
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.symbolic.SymbolicState
import org.utbot.engine.symbolic.SymbolicStateUpdate

// TODO: refactor this to `symbolic state` visitor or something like this when engine will be refactored.
interface PostConditionConstructor {
    fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult? // TODO: refactor this with `symbolic state` (this, result, parameters, statics)
    ): SymbolicStateUpdate

    fun constructSoftPostCondition(
        engine: UtBotSymbolicEngine
    ): SymbolicStateUpdate
}

internal object EmptyPostCondition : PostConditionConstructor {
    override fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = SymbolicStateUpdate()

    override fun constructSoftPostCondition(
        engine: UtBotSymbolicEngine
    ): SymbolicStateUpdate = SymbolicStateUpdate()
}

