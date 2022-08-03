package org.utbot.framework.plugin.api

interface UtConstraintVariableVisitor<T> {
    fun visitUtConstraintParameter(expr: UtConstraintParameter): T
    fun visitUtConstraintNull(expr: UtConstraintNull): T
    fun visitUtConstraintFieldAccess(expr: UtConstraintFieldAccess): T
    fun visitUtConstraintArrayAccess(expr: UtConstraintArrayAccess): T
    fun visitUtConstraintArrayLengthAccess(expr: UtConstraintArrayLength): T
    fun visitUtConstraintBoolConstant(expr: UtConstraintBoolConstant): T
    fun visitUtConstraintCharConstant(expr: UtConstraintCharConstant): T
    fun visitUtConstraintNumericConstant(expr: UtConstraintNumericConstant): T
    fun visitUtConstraintAdd(expr: UtConstraintAdd): T
    fun visitUtConstraintAnd(expr: UtConstraintAnd): T
    fun visitUtConstraintCmp(expr: UtConstraintCmp): T
    fun visitUtConstraintCmpg(expr: UtConstraintCmpg): T
    fun visitUtConstraintCmpl(expr: UtConstraintCmpl): T
    fun visitUtConstraintDiv(expr: UtConstraintDiv): T
    fun visitUtConstraintMul(expr: UtConstraintMul): T
    fun visitUtConstraintOr(expr: UtConstraintOr): T
    fun visitUtConstraintRem(expr: UtConstraintRem): T
    fun visitUtConstraintShl(expr: UtConstraintShl): T
    fun visitUtConstraintShr(expr: UtConstraintShr): T
    fun visitUtConstraintSub(expr: UtConstraintSub): T
    fun visitUtConstraintUshr(expr: UtConstraintUshr): T
    fun visitUtConstraintXor(expr: UtConstraintXor): T
    fun visitUtConstraintNot(expr: UtConstraintNot): T

    fun visitUtConstraintNeg(expr: UtConstraintNeg): T

    fun visitUtConstraintCast(expr: UtConstraintCast): T
}

interface UtConstraintVisitor<T> {
    fun visitUtRefEqConstraint(expr: UtRefEqConstraint): T
    fun visitUtRefNeqConstraint(expr: UtRefNeqConstraint): T
    fun visitUtRefTypeConstraint(expr: UtRefTypeConstraint): T
    fun visitUtRefNotTypeConstraint(expr: UtRefNotTypeConstraint): T

    fun visitUtBoolConstraint(expr: UtBoolConstraint): T
    fun visitUtEqConstraint(expr: UtEqConstraint): T
    fun visitUtNeqConstraint(expr: UtNeqConstraint): T
    fun visitUtLtConstraint(expr: UtLtConstraint): T
    fun visitUtGtConstraint(expr: UtGtConstraint): T
    fun visitUtLeConstraint(expr: UtLeConstraint): T
    fun visitUtGeConstraint(expr: UtGeConstraint): T
    fun visitUtAndConstraint(expr: UtAndConstraint): T
    fun visitUtOrConstraint(expr: UtOrConstraint): T
}
