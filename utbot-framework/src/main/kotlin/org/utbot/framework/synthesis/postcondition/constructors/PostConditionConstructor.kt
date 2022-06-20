package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.SymbolicResult
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.symbolic.SymbolicStateUpdate

// TODO: refactor this to `symbolic state` visitor or something like this when engine will be refactored.
fun interface PostConditionConstructor {
    fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult? // TODO: refactor this with `symbolic state` (this, result, parameters, statics)
    ): SymbolicStateUpdate
}

internal object EmptyPostCondition : PostConditionConstructor {
    override fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = SymbolicStateUpdate()
}

