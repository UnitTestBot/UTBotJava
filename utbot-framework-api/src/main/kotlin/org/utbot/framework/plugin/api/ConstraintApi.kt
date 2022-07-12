package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isPrimitive

sealed class Var {
    abstract val classId: ClassId
    val isPrimitive get() = classId.isPrimitive
}

data class NullVar(override val classId: ClassId) : Var() {
    override fun toString(): String = "null"
}

data class Parameter(
    val name: String,
    override val classId: ClassId
) : Var() {
    override fun toString(): String = name
}

data class FieldAccess(
    val instance: Var,
    val fieldId: FieldId,
) : Var() {
    override val classId: ClassId
        get() = fieldId.type

    override fun toString(): String = "$instance.${fieldId.name}"
}

data class ArrayAccess(
    val instance: Var,
    val index: Var,
    override val classId: ClassId
) : Var() {
    override fun toString(): String = "$instance[$index]"
}

data class ArrayLengthAccess(
    val instance: Var,
) : Var() {
    override val classId: ClassId = Integer.TYPE.id
    override fun toString(): String = "$instance.length"
}

data class BoolConstant(
    val value: Boolean
) : Var() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"
}

data class CharConstant(
    val value: Char,
) : Var() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"
}

data class NumericConstant(
    val value: Number,
) : Var() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"
}


sealed class UtConstraint {
    abstract fun negated(): UtConstraint
}

sealed class UtReferenceConstraint : UtConstraint()

data class UtRefEqConstraint(val lhv: Var, val rhv: Var) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefNeqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"
}

data class UtRefNeqConstraint(val lhv: Var, val rhv: Var) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefEqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"
}

data class UtRefTypeConstraint(val operand: Var, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefNotTypeConstraint(operand, type)

    override fun toString(): String = "$operand is $type"
}

data class UtRefNotTypeConstraint(val operand: Var, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefTypeConstraint(operand, type)

    override fun toString(): String = "$operand !is $type"
}

sealed class UtPrimitiveConstraint : UtConstraint()

data class UtEqConstraint(val lhv: Var, val rhv: Var) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtNeqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"
}

data class UtNeqConstraint(val lhv: Var, val rhv: Var) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtEqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"
}

data class UtLtConstraint(val lhv: Var, val rhv: Var) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtGeConstraint(lhv, rhv)

    override fun toString(): String = "$lhv < $rhv"
}

data class UtGtConstraint(val lhv: Var, val rhv: Var) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtLeConstraint(lhv, rhv)

    override fun toString(): String = "$lhv > $rhv"
}

data class UtLeConstraint(val lhv: Var, val rhv: Var) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtGtConstraint(lhv, rhv)

    override fun toString(): String = "$lhv <= $rhv"
}

data class UtGeConstraint(val lhv: Var, val rhv: Var) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtLtConstraint(lhv, rhv)

    override fun toString(): String = "$lhv >= $rhv"
}

data class UtAndConstraint(val lhv: UtConstraint, val rhv: UtConstraint) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtOrConstraint(lhv.negated(), rhv.negated())

    override fun toString(): String = "($lhv) && ($rhv)"
}

data class UtOrConstraint(val lhv: UtConstraint, val rhv: UtConstraint) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtAndConstraint(lhv.negated(), rhv.negated())

    override fun toString(): String = "($lhv) || ($rhv)"
}