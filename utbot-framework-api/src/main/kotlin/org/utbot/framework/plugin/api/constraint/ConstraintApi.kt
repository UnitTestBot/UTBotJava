package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.constraint.UtConstraintVariableCollector
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

data class UtConstraintNeg(
    val operand: UtConstraintVariable
) : UtConstraintExpr() {
    override val classId: ClassId
        get() = operand.classId

    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintNeg(this)
    }
}

data class UtConstraintCast(
    val operand: UtConstraintVariable,
    override val classId: ClassId
) : UtConstraintExpr() {
    override fun <T> accept(visitor: UtConstraintVariableVisitor<T>): T {
        return visitor.visitUtConstraintCast(this)
    }
}

sealed class UtConstraint {
    abstract fun negated(): UtConstraint
    abstract fun <T> accept(visitor: UtConstraintVisitor<T>): T
}

data class UtNegatedConstraint(val constraint: UtConstraint) : UtConstraint() {
    override fun negated(): UtConstraint = constraint
    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtNegatedConstraint(this)
    }

    override fun toString(): String = "!($constraint)"
}

sealed class UtReferenceConstraint : UtConstraint()

data class UtRefEqConstraint(val lhv: UtConstraintVariable, val rhv: UtConstraintVariable) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtNegatedConstraint(this)

    override fun toString(): String = "$lhv == $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefEqConstraint(this)
    }
}

data class UtRefGenericEqConstraint(
    val lhv: UtConstraintVariable,
    val rhv: UtConstraintVariable,
    val mapping: Map<Int, Int>
) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtNegatedConstraint(this)

    override fun toString(): String = "$lhv == $rhv <$mapping>"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefGenericEqConstraint(this)
    }
}

data class UtRefTypeConstraint(val operand: UtConstraintVariable, val type: ClassId) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtNegatedConstraint(this)

    override fun toString(): String = "$operand is $type"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefTypeConstraint(this)
    }
}

data class UtRefGenericTypeConstraint(
    val operand: UtConstraintVariable,
    val base: UtConstraintVariable,
    val parameterIndex: Int
) : UtReferenceConstraint() {
    override fun negated(): UtConstraint = UtNegatedConstraint(this)

    override fun toString(): String = "$operand is $base<$parameterIndex>"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtRefGenericTypeConstraint(this)
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
    override fun negated(): UtConstraint = UtNegatedConstraint(this)

    override fun toString(): String = "$lhv == $rhv"

    override fun <T> accept(visitor: UtConstraintVisitor<T>): T {
        return visitor.visitUtEqConstraint(this)
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


fun UtConstraint.flatMap() = flatMap { true }
fun UtConstraint.flatMap(predicate: (UtConstraintVariable) -> Boolean) =
    this.accept(UtConstraintVariableCollector(predicate))

operator fun UtConstraint.contains(variable: UtConstraintVariable) = this.accept(UtConstraintVariableCollector {
    it == variable
}).isNotEmpty()

operator fun UtConstraint.contains(variables: Set<UtConstraintVariable>) =
    this.accept(UtConstraintVariableCollector {
        it in variables
    }).isNotEmpty()

data class ConstrainedExecution(
    val modelsBefore: List<UtModel>,
    val modelsAfter: List<UtModel>
)


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