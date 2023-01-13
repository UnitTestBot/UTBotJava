package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.getOrigin

enum class ConstraintKind {
    LowerBound,
    UpperBound,
    BothSided
}

fun reverseConstraintKind(kind: ConstraintKind): ConstraintKind {
    return when (kind) {
        ConstraintKind.LowerBound -> ConstraintKind.UpperBound
        ConstraintKind.UpperBound -> ConstraintKind.LowerBound
        ConstraintKind.BothSided -> ConstraintKind.BothSided
    }
}

data class TypeConstraint(
    val type: Type,
    val kind: ConstraintKind
)

// key: index of annotationParameter
fun propagateConstraint(type: Type, constraint: TypeConstraint, strict: Boolean = false): Map<Int, TypeConstraint> {
    return when (val description = type.pythonDescription()) {
        is PythonAnyTypeDescription, is PythonNoneTypeDescription, is PythonTypeVarDescription -> emptyMap()
        is PythonOverloadTypeDescription -> emptyMap() // TODO
        is PythonCallableTypeDescription -> emptyMap() // TODO
        is PythonUnionTypeDescription -> emptyMap() // TODO
        is PythonTupleTypeDescription -> emptyMap() // TODO
        is PythonCompositeTypeDescription -> {
            propagateConstraintForCompositeType(type, description, constraint)
        }
    }
}

private fun propagateConstraintForCompositeType(
    type: Type,
    description: PythonCompositeTypeDescription,
    constraint: TypeConstraint
): Map<Int, TypeConstraint> {
    return when (val constraintDescription = constraint.type.pythonDescription()) {
        is PythonCompositeTypeDescription -> {
            if (constraintDescription.name != description.name)
                return emptyMap()  // TODO: consider some cases here
            val origin = type.getOrigin()
            (constraint.type.parameters zip origin.parameters).mapIndexed { index, (constraintValue, param) ->
                if ((param.pythonDescription() as? PythonTypeVarDescription)?.variance == PythonTypeVarDescription.Variance.COVARIANT) {
                    return@mapIndexed Pair(index, TypeConstraint(constraintValue, constraint.kind))
                }
                if ((param.pythonDescription() as? PythonTypeVarDescription)?.variance == PythonTypeVarDescription.Variance.CONTRAVARIANT) {
                    return@mapIndexed Pair(index, TypeConstraint(constraintValue, reverseConstraintKind(constraint.kind)))
                }
                return@mapIndexed Pair(index, TypeConstraint(constraintValue, ConstraintKind.BothSided))
            }.associate { it }
        }
        else -> emptyMap() // TODO: consider some other cases here
    }
}