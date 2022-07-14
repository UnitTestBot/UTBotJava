package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive

sealed class UtConstraintVariable {
    abstract val classId: ClassId
    val isPrimitive get() = classId.isPrimitive
    val isArray get() = classId.isArray
}

data class NullUtConstraintVariable(override val classId: ClassId) : UtConstraintVariable() {
    override fun toString(): String = "null"
}

data class UtConstraintParameter(
    val name: String,
    override val classId: ClassId
) : UtConstraintVariable() {
    override fun toString(): String = name
}

data class UtConstraintFieldAccess(
    val instance: UtConstraintVariable,
    val fieldId: FieldId,
) : UtConstraintVariable() {
    override val classId: ClassId
        get() = fieldId.type

    override fun toString(): String = "$instance.${fieldId.name}"
}

data class UtConstraintArrayAccess(
    val instance: UtConstraintVariable,
    val index: UtConstraintVariable,
    override val classId: ClassId
) : UtConstraintVariable() {
    override fun toString(): String = "$instance[$index]"
}

data class UtConstraintArrayLengthAccess(
    val instance: UtConstraintVariable,
) : UtConstraintVariable() {
    override val classId: ClassId = Integer.TYPE.id
    override fun toString(): String = "$instance.length"
}

data class UtConstraintBoolConstant(
    val value: Boolean
) : UtConstraintVariable() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"
}

data class UtConstraintCharConstant(
    val value: Char,
) : UtConstraintVariable() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"
}

data class UtConstraintNumericConstant(
    val value: Number,
) : UtConstraintVariable() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"
}


sealed class UtConstraint {
    abstract fun negated(): UtConstraint
}

sealed class UtReferenceConstraint : UtConstraint()

data class UtRefEqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefNeqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"
}

data class UtRefNeqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefEqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"
}

data class UtRefTypeConstraint(val operand: UtConstraintVariable, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefNotTypeConstraint(operand, type)

    override fun toString(): String = "$operand is $type"
}

data class UtRefNotTypeConstraint(val operand: UtConstraintVariable, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefTypeConstraint(operand, type)

    override fun toString(): String = "$operand !is $type"
}

sealed class UtPrimitiveConstraint : UtConstraint()

data class UtEqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtNeqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"
}

data class UtNeqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtEqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"
}

data class UtLtConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtGeConstraint(lhv, rhv)

    override fun toString(): String = "$lhv < $rhv"
}

data class UtGtConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtLeConstraint(lhv, rhv)

    override fun toString(): String = "$lhv > $rhv"
}

data class UtLeConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtGtConstraint(lhv, rhv)

    override fun toString(): String = "$lhv <= $rhv"
}

data class UtGeConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
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