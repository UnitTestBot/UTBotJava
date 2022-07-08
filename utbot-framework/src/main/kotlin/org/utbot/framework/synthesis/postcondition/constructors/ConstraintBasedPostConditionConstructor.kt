package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.ResolvedObject
import org.utbot.engine.SymbolicResult
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.symbolic.asSoftConstraint

class ConstraintBasedPostConditionConstructor(
    private val parameters: List<ResolvedObject>
) : PostConditionConstructor {

    override fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = SymbolicStateUpdate(
        hardConstraints = parameters.flatMap { it.constraints }.asHardConstraint()
    )

    override fun constructSoftPostCondition(
        engine: UtBotSymbolicEngine
    ): SymbolicStateUpdate = SymbolicStateUpdate(
        softConstraints = parameters.flatMap { it.constraints }.asSoftConstraint()
    )
}