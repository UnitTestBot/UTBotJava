package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.*

sealed class UtConstraintVariable {
    abstract val classId: ClassId
    val isPrimitive get() = classId.isPrimitive
    val isArray get() = classId.isArray

    abstract fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T
}

data class UtConstraintNull(override val classId: ClassId) : UtConstraintVariable() {
    override fun toString(): String = "null"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintNull(this)
    }
}

data class UtConstraintParameter(
    val name: String,
    override val classId: ClassId
) : UtConstraintVariable() {
    override fun toString(): String = name

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintParameter(this)
    }
}

data class UtConstraintFieldAccess(
    val instance: UtConstraintVariable,
    val fieldId: FieldId,
) : UtConstraintVariable() {
    override val classId: ClassId
        get() = fieldId.type

    override fun toString(): String = "$instance.${fieldId.name}"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintFieldAccess(this)
    }
}

data class UtConstraintArrayAccess(
    val instance: UtConstraintVariable,
    val index: UtConstraintVariable,
    override val classId: ClassId
) : UtConstraintVariable() {
    override fun toString(): String = "$instance[$index]"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintArrayAccess(this)
    }
}

data class UtConstraintArrayLength(
    val instance: UtConstraintVariable,
) : UtConstraintVariable() {
    override val classId: ClassId = Integer.TYPE.id
    override fun toString(): String = "$instance.length"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintArrayLengthAccess(this)
    }
}

data class UtConstraintBoolConstant(
    val value: Boolean
) : UtConstraintVariable() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintBoolConstant(this)
    }
}

data class UtConstraintCharConstant(
    val value: Char,
) : UtConstraintVariable() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintCharConstant(this)
    }
}

data class UtConstraintNumericConstant(
    val value: Number,
) : UtConstraintVariable() {
    override val classId: ClassId = primitiveModelValueToClassId(value)

    override fun toString(): String = "$value"

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintNumericConstant(this)
    }
}

sealed class UtConstraintExpr : UtConstraintVariable()

sealed class UtConstraintBinExpr(
    open val lhv: UtConstraintVariable,
    open val rhv: UtConstraintVariable
) : UtConstraintExpr()

data class UtConstraintAdd(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintAdd(this)
    }
}

data class UtConstraintAnd(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintAnd(this)
    }
}

data class UtConstraintCmp(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = intClassId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintCmp(this)
    }
}

data class UtConstraintCmpg(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = intClassId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintCmpg(this)
    }
}

data class UtConstraintCmpl(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = intClassId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintCmpl(this)
    }
}

data class UtConstraintDiv(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintDiv(this)
    }
}

data class UtConstraintMul(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintMul(this)
    }
}

data class UtConstraintOr(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintOr(this)
    }
}

data class UtConstraintRem(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintRem(this)
    }
}

data class UtConstraintShl(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintShl(this)
    }
}

data class UtConstraintShr(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintShr(this)
    }
}

data class UtConstraintSub(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintSub(this)
    }
}

data class UtConstraintUshr(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintUshr(this)
    }
}

data class UtConstraintXor(
    override val lhv: UtConstraintVariable,
    override val rhv: UtConstraintVariable
) : UtConstraintBinExpr(lhv, rhv) {
    override val classId: ClassId
        get() = lhv.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintXor(this)
    }
}

data class UtConstraintNot(
    val operand: UtConstraintVariable
) : UtConstraintExpr() {
    override val classId: ClassId
        get() = operand.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintNot(this)
    }
}

sealed class UtConstraint {
    abstract fun negated(): UtConstraint
    abstract fun <T> accept(visitor: UtConstraintVisitor<T>): T
}

sealed class UtReferenceConstraint : UtConstraint()

data class UtRefEqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefNeqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefEqConstraint(this)
    }
}

data class UtRefNeqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefEqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefNeqConstraint(this)
    }
}

data class UtRefTypeConstraint(val operand: UtConstraintVariable, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefNotTypeConstraint(operand, type)

    override fun toString(): String = "$operand is $type"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefTypeConstraint(this)
    }
}

data class UtRefNotTypeConstraint(val operand: UtConstraintVariable, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtRefTypeConstraint(operand, type)

    override fun toString(): String = "$operand !is $type"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefNotTypeConstraint(this)
    }
}

sealed class UtPrimitiveConstraint : UtConstraint()

data class UtBoolConstraint(val operand: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtBoolConstraint(UtConstraintNot(operand))

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtBoolConstraint(this)
    }
}

data class UtEqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtNeqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv == $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtEqConstraint(this)
    }
}

data class UtNeqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtEqConstraint(lhv, rhv)

    override fun toString(): String = "$lhv != $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtNeqConstraint(this)
    }
}

data class UtLtConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtGeConstraint(lhv, rhv)

    override fun toString(): String = "$lhv < $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtLtConstraint(this)
    }
}

data class UtGtConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtLeConstraint(lhv, rhv)

    override fun toString(): String = "$lhv > $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtGtConstraint(this)
    }
}

data class UtLeConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtGtConstraint(lhv, rhv)

    override fun toString(): String = "$lhv <= $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtLeConstraint(this)
    }
}

data class UtGeConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtLtConstraint(lhv, rhv)

    override fun toString(): String = "$lhv >= $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtGeConstraint(this)
    }
}

data class UtAndConstraint(val lhv: UtConstraint, val rhv: UtConstraint) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtOrConstraint(lhv.negated(), rhv.negated())

    override fun toString(): String = "($lhv) && ($rhv)"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtAndConstraint(this)
    }
}

data class UtOrConstraint(val lhv: UtConstraint, val rhv: UtConstraint) : UtPrimitiveConstraint() {
    override fun negated(): UtConstraint = UtAndConstraint(lhv.negated(), rhv.negated())

    override fun toString(): String = "($lhv) || ($rhv)"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtOrConstraint(this)
    }
}


val ClassId.defaultVariable: UtConstraintVariable
    get() = when (this) {
        voidClassId -> error("Unexpected")
        booleanClassId -> UtConstraintBoolConstant(false)
        charClassId -> UtConstraintCharConstant(0.toChar())
        byteClassId -> UtConstraintNumericConstant(0.toByte())
        shortClassId -> UtConstraintNumericConstant(0.toShort())
        intClassId -> UtConstraintNumericConstant(0)
        longClassId -> UtConstraintNumericConstant(0.toLong())
        floatClassId -> UtConstraintNumericConstant(0.toFloat())
        doubleClassId -> UtConstraintNumericConstant(0.toDouble())
        else -> UtConstraintNull(this)
    }

val ClassId.defaultValue: Any
    get() = when (this) {
        voidClassId -> Unit
        booleanClassId -> false
        charClassId -> 0.toChar()
        byteClassId -> 0.toByte()
        shortClassId -> 0.toShort()
        intClassId -> 0
        longClassId -> 0.toLong()
        floatClassId -> 0.toFloat()
        doubleClassId -> 0.toDouble()
        else -> UtNullModel(this)
    }