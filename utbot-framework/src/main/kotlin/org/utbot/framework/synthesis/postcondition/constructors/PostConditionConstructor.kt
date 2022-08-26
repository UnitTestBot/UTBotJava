package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.*
import org.utbot.engine.selectors.strategies.ScoringStrategyBuilder
import org.utbot.engine.selectors.strategies.defaultScoringStrategy
import org.utbot.engine.symbolic.SymbolicState
import org.utbot.engine.symbolic.SymbolicStateUpdate

// TODO: refactor this to `symbolic state` visitor or something like this when engine will be refactored.
interface PostConditionConstructor {
    fun constructPostCondition(
        traverser: Traverser,
        symbolicResult: SymbolicResult? // TODO: refactor this with `symbolic state` (this, result, parameters, statics)
    ): SymbolicStateUpdate

    fun constructSoftPostCondition(
        traverser: Traverser,
    ): SymbolicStateUpdate

    fun scoringBuilder(): ScoringStrategyBuilder
}

internal object EmptyPostCondition : PostConditionConstructor {
    override fun constructPostCondition(
        traverser: Traverser,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = SymbolicStateUpdate()

    override fun constructSoftPostCondition(
        traverser: Traverser,
    ): SymbolicStateUpdate = SymbolicStateUpdate()

    override fun scoringBuilder(): ScoringStrategyBuilder = defaultScoringStrategy
}

