package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.Type

enum class ConstraintType {
    LowerBound,
    UpperBound,
    BothSided
}

data class TypeConstraint(
    val type: Type,
    val constraintType: ConstraintType
)