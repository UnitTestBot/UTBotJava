package org.utbot.engine

import org.utbot.framework.plugin.api.*

class UtConstraintVariableCollector(
    val predicate: (UtConstraintVariable) -> Boolean
) : UtConstraintVisitor<Set<UtConstraintVariable>> {
    private val result = mutableSetOf<UtConstraintVariable>()

    private inline fun visitVar(expr: UtConstraintVariable, body: () -> Unit): Set<UtConstraintVariable> {
        if (predicate(expr)) result += expr
        body()
        return result
    }

    private inline fun visitConstraint(expr: UtConstraint, body: () -> Unit): Set<UtConstraintVariable> {
        body()
        return result
    }


    override fun visitUtConstraintParameter(expr: UtConstraintParameter) = visitVar(expr) {}

    override fun visitUtConstraintNull(expr: UtConstraintNull) = visitVar(expr) {}

    override fun visitUtConstraintFieldAccess(expr: UtConstraintFieldAccess) = visitVar(expr) {
        expr.instance.accept(this)
    }

    override fun visitUtConstraintArrayAccess(expr: UtConstraintArrayAccess) = visitVar(expr) {
        expr.instance.accept(this)
        expr.index.accept(this)
    }

    override fun visitUtConstraintArrayLengthAccess(expr: UtConstraintArrayLength) = visitVar(expr) {
        expr.instance.accept(this)
    }

    override fun visitUtConstraintBoolConstant(expr: UtConstraintBoolConstant) = visitVar(expr) {}

    override fun visitUtConstraintCharConstant(expr: UtConstraintCharConstant) = visitVar(expr) {}

    override fun visitUtConstraintNumericConstant(expr: UtConstraintNumericConstant) = visitVar(expr) {}

    override fun visitUtConstraintAdd(expr: UtConstraintAdd) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintAnd(expr: UtConstraintAnd) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintCmp(expr: UtConstraintCmp) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintCmpg(expr: UtConstraintCmpg) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintCmpl(expr: UtConstraintCmpl) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintDiv(expr: UtConstraintDiv) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintMul(expr: UtConstraintMul) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintOr(expr: UtConstraintOr) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintRem(expr: UtConstraintRem) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintShl(expr: UtConstraintShl) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintShr(expr: UtConstraintShr) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintSub(expr: UtConstraintSub) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintUshr(expr: UtConstraintUshr) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintXor(expr: UtConstraintXor) = visitVar(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtConstraintNot(expr: UtConstraintNot) = visitVar(expr) {
        expr.operand.accept(this)
    }

    override fun visitUtRefEqConstraint(expr: UtRefEqConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtRefNeqConstraint(expr: UtRefNeqConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtRefTypeConstraint(expr: UtRefTypeConstraint) = visitConstraint(expr) {
        expr.operand.accept(this)
    }

    override fun visitUtRefNotTypeConstraint(expr: UtRefNotTypeConstraint) = visitConstraint(expr) {
        expr.operand.accept(this)
    }

    override fun visitUtBoolConstraint(expr: UtBoolConstraint) = visitConstraint(expr) {
        expr.operand.accept(this)
    }

    override fun visitUtEqConstraint(expr: UtEqConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtNeqConstraint(expr: UtNeqConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtLtConstraint(expr: UtLtConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtGtConstraint(expr: UtGtConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtLeConstraint(expr: UtLeConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtGeConstraint(expr: UtGeConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtAndConstraint(expr: UtAndConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }

    override fun visitUtOrConstraint(expr: UtOrConstraint) = visitConstraint(expr) {
        expr.lhv.accept(this)
        expr.rhv.accept(this)
    }
}