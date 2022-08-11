package org.utbot.engine.pc

import org.utbot.engine.*
import org.utbot.engine.Eq
import org.utbot.engine.Ge
import org.utbot.engine.Le
import org.utbot.engine.Lt
import org.utbot.engine.Ne
import org.utbot.engine.pc.constraint.UtDefaultExpressionVisitor
import org.utbot.engine.pc.constraint.UtVarContext
import org.utbot.engine.z3.boolValue
import org.utbot.engine.z3.value
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.objectClassId

class NotSupportedByConstraintResolverException : Exception()

class UtConstraintBuilder(
    val variableContext: UtVarContext
) : UtDefaultExpressionVisitor<UtConstraint?>({ throw NotSupportedByConstraintResolverException() }) {
    val holder get() = variableContext.holder

    private fun shouldSkip(expr: UtExpression): Boolean {
        if ("addrToType" in expr.toString()) return true
        if ("addrToNumDimensions" in expr.toString()) return true
        if ("isMock" in expr.toString()) return true
        if ("arraySetRange" in expr.toString()) return true
        return false
    }

    private fun applyConstraint(expr: UtExpression, constraint: () -> UtConstraint?): UtConstraint? =
        when (holder.eval(expr).value()) {
            true -> constraint()
            false -> constraint()?.negated()
            else -> error("Not boolean expr")
        }

    override fun visit(expr: UtArraySelectExpression): UtConstraint? {
        if (shouldSkip(expr)) return null
        return super.visit(expr)
    }

    override fun visit(expr: UtTrue): UtConstraint {
        return UtBoolConstraint(UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtFalse): UtConstraint {
        return UtBoolConstraint(UtConstraintBoolConstant(false))
    }

    override fun visit(expr: UtEqExpression): UtConstraint? = applyConstraint(expr) {
        if (shouldSkip(expr)) return@applyConstraint null

        val lhv = expr.left.accept(variableContext)
        val rhv = expr.right.accept(variableContext)
        when {
            lhv.isPrimitive && rhv.isPrimitive -> UtEqConstraint(lhv, rhv)
            else -> UtRefEqConstraint(lhv, rhv)
        }
    }

    override fun visit(expr: UtBoolConst): UtConstraint = applyConstraint(expr) {
        UtBoolConstraint(expr.accept(variableContext))
    }!!

    override fun visit(expr: NotBoolExpression): UtConstraint = applyConstraint(expr) {
        UtBoolConstraint(
            UtConstraintNot(expr.expr.accept(variableContext))
        )
    }!!

    override fun visit(expr: UtOrBoolExpression): UtConstraint = applyConstraint(expr) {
        val vars = expr.exprs.map { it.accept(variableContext) }
        UtBoolConstraint(
            when {
                vars.isEmpty() -> UtConstraintBoolConstant(true)
                vars.size == 1 -> vars.first()
                else -> vars.reduce { acc, variable -> UtConstraintOr(acc, variable) }
            }
        )
    }!!

    override fun visit(expr: UtAndBoolExpression): UtConstraint = applyConstraint(expr) {
        val vars = expr.exprs.map { it.accept(variableContext) }
        UtBoolConstraint(
            when {
                vars.isEmpty() -> UtConstraintBoolConstant(true)
                vars.size == 1 -> vars.first()
                else -> vars.reduce { acc, variable -> UtConstraintAnd(acc, variable) }
            }
        )
    }!!

    override fun visit(expr: UtBoolOpExpression): UtConstraint? = applyConstraint(expr) {
        if (shouldSkip(expr)) return@applyConstraint null
        val lhv = expr.left.expr.accept(variableContext)
        val rhv = expr.right.expr.accept(variableContext)
        when (expr.operator) {
            Le -> {
                if (!lhv.isPrimitive && !rhv.isPrimitive) return@applyConstraint null
                UtLeConstraint(lhv, rhv)
            }

            Lt -> {
                if (!lhv.isPrimitive && !rhv.isPrimitive) return@applyConstraint null
                UtLtConstraint(lhv, rhv)
            }

            Ge -> {
                if (!lhv.isPrimitive && !rhv.isPrimitive) return@applyConstraint null
                UtGeConstraint(lhv, rhv)
            }

            Gt -> {
                if (!lhv.isPrimitive && !rhv.isPrimitive) return@applyConstraint null
                UtGtConstraint(lhv, rhv)
            }

            Eq -> when (lhv.isPrimitive && rhv.isPrimitive) {
                true -> UtEqConstraint(lhv, rhv)
                false -> UtRefEqConstraint(lhv, rhv.wrapToRef())
            }

            Ne -> when (lhv.isPrimitive && rhv.isPrimitive) {
                true -> UtEqConstraint(lhv, rhv).negated()
                false -> UtEqConstraint(lhv, rhv.wrapToRef()).negated()
            }
        }
    }

    fun UtConstraintVariable.wrapToRef(): UtConstraintVariable = when (this) {
        is UtConstraintNumericConstant -> when (this.value) {
            0 -> UtConstraintNull(objectClassId)
            else -> error("Unexpected")
        }
        else -> this
    }

    override fun visit(expr: UtIsExpression): UtConstraint? {
        if (shouldSkip(expr)) return null
        val operand = expr.addr.accept(variableContext)
        return UtRefTypeConstraint(operand, expr.type.classId)
    }

    override fun visit(expr: UtGenericExpression): UtConstraint {
        throw NotSupportedByConstraintResolverException()
    }

    override fun visit(expr: UtIsGenericTypeExpression): UtConstraint = applyConstraint(expr) {
        UtRefGenericTypeConstraint(
            expr.addr.accept(variableContext),
            expr.baseAddr.accept(variableContext),
            expr.parameterTypeIndex
        )
    }!!

    override fun visit(expr: UtEqGenericTypeParametersExpression): UtConstraint? = applyConstraint(expr) {
        if (shouldSkip(expr)) return@applyConstraint null

        val lhv = expr.firstAddr.accept(variableContext)
        val rhv = expr.secondAddr.accept(variableContext)
        UtRefGenericEqConstraint(lhv, rhv, expr.indexMapping)
    }

    override fun visit(expr: UtInstanceOfExpression): UtConstraint? = applyConstraint(expr) {
        expr.constraint.accept(this)
    }

    override fun visit(expr: UtIteExpression): UtConstraint? = applyConstraint(expr) {
        val condValue = holder.eval(expr.condition).boolValue()
        assert(expr.thenExpr.sort is UtBoolSort)
        assert(expr.elseExpr.sort is UtBoolSort)
        when {
            condValue -> expr.thenExpr.accept(this)
            else -> expr.elseExpr.accept(this)
        }
    }
}