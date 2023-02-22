package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.TypeParameter
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

// key of returned Map: index of annotationParameter
fun propagateConstraint(type: Type, constraint: TypeConstraint, storage: PythonTypeStorage): Map<Int, TypeConstraint> {
    return when (val description = type.pythonDescription()) {
        is PythonAnyTypeDescription, is PythonNoneTypeDescription, is PythonTypeVarDescription -> emptyMap()
        is PythonOverloadTypeDescription -> emptyMap() // TODO
        is PythonCallableTypeDescription -> emptyMap() // TODO
        is PythonUnionTypeDescription -> emptyMap() // TODO
        is PythonTupleTypeDescription -> emptyMap() // TODO
        is PythonProtocolDescription -> emptyMap() // TODO
        is PythonTypeAliasDescription -> emptyMap() // TODO
        is PythonConcreteCompositeTypeDescription -> {
            propagateConstraintForCompositeType(type, description, constraint, storage)
        }
    }
}

private fun propagateConstraintForCompositeType(
    type: Type,
    description: PythonCompositeTypeDescription,
    constraint: TypeConstraint,
    storage: PythonTypeStorage
): Map<Int, TypeConstraint> {
    return when (val constraintDescription = constraint.type.pythonDescription()) {
        is PythonConcreteCompositeTypeDescription -> {
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
        is PythonProtocolDescription -> {
            val collectedConstraints = mutableMapOf<Type, TypeConstraint>()
            val origin = type.getOrigin()
            constraintDescription.protocolMemberNames.forEach {
                val abstractAttr = constraint.type.getPythonAttributeByName(storage, it) ?: return@forEach
                val concreteAttr = origin.getPythonAttributeByName(storage, it) ?: return@forEach
                // TODO: consider more cases
                when (val desc = concreteAttr.type.pythonDescription()) {
                    is PythonTypeVarDescription -> {
                        collectedConstraints[concreteAttr.type] =
                            TypeConstraint(abstractAttr.type, ConstraintKind.UpperBound)
                    }
                    is PythonCallableTypeDescription -> {
                        val typeOfAbstract = abstractAttr.type
                        if (typeOfAbstract !is FunctionType)
                            return@forEach
                        val callable = desc.castToCompatibleTypeApi(concreteAttr.type)
                        (callable.arguments zip typeOfAbstract.arguments).forEach { (arg, abs) ->
                            if (arg is TypeParameter)
                                collectedConstraints[arg] = TypeConstraint(abs, ConstraintKind.UpperBound)
                        }
                        if (callable.returnValue is TypeParameter)
                            collectedConstraints[callable.returnValue] =
                                TypeConstraint(typeOfAbstract.returnValue, ConstraintKind.LowerBound)
                    }
                    else -> {}
                }
            }
            origin.parameters.mapIndexedNotNull { ind, param ->
                collectedConstraints[param]?.let { Pair(ind, it) }
            }.associate { it }
        }
        else -> emptyMap() // TODO: consider some other cases here
    }
}