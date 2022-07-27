package org.utbot.engine

import org.utbot.framework.plugin.api.*

class UtConstraintTransformer(
    val mapping: Map<UtConstraintVariable, UtConstraintVariable>
) : UtConstraintVisitor<UtConstraint>, UtConstraintVariableVisitor<UtConstraintVariable> {

    private inline fun <reified T : UtConstraintVariable> replace(
        expr: T,
        body: T.() -> UtConstraintVariable
    ): UtConstraintVariable = mapping.getOrElse(expr) { expr.body() }

    override fun visitUtConstraintParameter(expr: UtConstraintParameter) = replace(expr) { expr }

    override fun visitUtConstraintNull(expr: UtConstraintNull) = replace(expr) { expr }

    override fun visitUtConstraintFieldAccess(expr: UtConstraintFieldAccess) = replace(expr) {
        UtConstraintFieldAccess(
            instance.accept(this@UtConstraintTransformer),
            fieldId
        )
    }

    override fun visitUtConstraintArrayAccess(expr: UtConstraintArrayAccess) = replace(expr) {
        UtConstraintArrayAccess(
            instance.accept(this@UtConstraintTransformer),
            index.accept(this@UtConstraintTransformer),
            classId
        )
    }

    override fun visitUtConstraintArrayLengthAccess(expr: UtConstraintArrayLength) = replace(expr) {
        UtConstraintArrayLength(
            instance.accept(this@UtConstraintTransformer),
        )
    }

    override fun visitUtConstraintBoolConstant(expr: UtConstraintBoolConstant) = replace(expr) { expr }

    override fun visitUtConstraintCharConstant(expr: UtConstraintCharConstant) = replace(expr) { expr }

    override fun visitUtConstraintNumericConstant(expr: UtConstraintNumericConstant) = replace(expr) { expr }

    override fun visitUtConstraintAdd(expr: UtConstraintAdd) = replace(expr) {
        UtConstraintAdd(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintAnd(expr: UtConstraintAnd) = replace(expr) {
        UtConstraintAnd(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintCmp(expr: UtConstraintCmp) = replace(expr) {
        UtConstraintCmp(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintCmpg(expr: UtConstraintCmpg) = replace(expr) {
        UtConstraintCmpg(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintCmpl(expr: UtConstraintCmpl) = replace(expr) {
        UtConstraintCmpl(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintDiv(expr: UtConstraintDiv) = replace(expr) {
        UtConstraintDiv(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintMul(expr: UtConstraintMul) = replace(expr) {
        UtConstraintMul(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintOr(expr: UtConstraintOr) = replace(expr) {
        UtConstraintOr(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintRem(expr: UtConstraintRem) = replace(expr) {
        UtConstraintRem(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintShl(expr: UtConstraintShl) = replace(expr) {
        UtConstraintShl(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintShr(expr: UtConstraintShr) = replace(expr) {
        UtConstraintShr(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintSub(expr: UtConstraintSub) = replace(expr) {
        UtConstraintSub(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintUshr(expr: UtConstraintUshr) = replace(expr) {
        UtConstraintUshr(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintXor(expr: UtConstraintXor) = replace(expr) {
        UtConstraintXor(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtConstraintNot(expr: UtConstraintNot) = replace(expr) {
        UtConstraintNot(
            operand.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtRefEqConstraint(expr: UtRefEqConstraint) = with(expr) {
        UtRefEqConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtRefNeqConstraint(expr: UtRefNeqConstraint) = with(expr) {
        UtRefEqConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtRefTypeConstraint(expr: UtRefTypeConstraint) = with(expr) {
        UtRefTypeConstraint(
            operand.accept(this@UtConstraintTransformer),
            type
        )
    }

    override fun visitUtRefNotTypeConstraint(expr: UtRefNotTypeConstraint) = with(expr) {
        UtRefNotTypeConstraint(
            operand.accept(this@UtConstraintTransformer),
            type
        )
    }

    override fun visitUtBoolConstraint(expr: UtBoolConstraint) = with(expr) {
        UtBoolConstraint(
            operand.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtEqConstraint(expr: UtEqConstraint) = with(expr) {
        UtEqConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtNeqConstraint(expr: UtNeqConstraint) = with(expr) {
        UtNeqConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtLtConstraint(expr: UtLtConstraint) = with(expr) {
        UtLtConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtGtConstraint(expr: UtGtConstraint) = with(expr) {
        UtGtConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtLeConstraint(expr: UtLeConstraint) = with(expr) {
        UtLeConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtGeConstraint(expr: UtGeConstraint) = with(expr) {
        UtGeConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtAndConstraint(expr: UtAndConstraint) = with(expr) {
        UtAndConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }

    override fun visitUtOrConstraint(expr: UtOrConstraint) = with(expr) {
        UtOrConstraint(
            lhv.accept(this@UtConstraintTransformer),
            rhv.accept(this@UtConstraintTransformer)
        )
    }
}