package org.utbot.engine.pc

import org.utbot.engine.*
import org.utbot.engine.Eq
import org.utbot.engine.Ge
import org.utbot.engine.Le
import org.utbot.engine.Lt
import org.utbot.engine.Ne
import org.utbot.engine.z3.value
import org.utbot.framework.plugin.api.*

class UtConstraintBuilder(
    val varBuilder: UtVarBuilder
) : UtExpressionVisitor<UtConstraint?> {
    val holder get() = varBuilder.holder

    private fun shouldSkip(expr: UtExpression): Boolean {
        if ("addrToType" in expr.toString()) return true
        if ("addrToNumDimensions" in expr.toString()) return true
        if ("isMock" in expr.toString()) return true
        if ("org.utbot.engine.overrides.collections." in expr.toString()) return true
        return false
    }

    private fun applyConstraint(expr: UtExpression, constraint: () -> UtConstraint?): UtConstraint? = when (holder.eval(expr).value()) {
        true -> constraint()
        false -> constraint()?.negated()
        else -> error("Not boolean expr")
    }

    override fun visit(expr: UtArraySelectExpression): UtConstraint? {
        if (shouldSkip(expr)) return null
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtConstArrayExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtMkArrayExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayMultiStoreExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtBvLiteral): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtBvConst): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtAddrExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtFpLiteral): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtFpConst): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtOpExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtTrue): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtFalse): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtEqExpression): UtConstraint? = applyConstraint(expr) {
        if (shouldSkip(expr)) return@applyConstraint null

        val lhv = expr.left.accept(varBuilder)
        val rhv = expr.right.accept(varBuilder)
        when {
            lhv.isPrimitive && rhv.isPrimitive ->UtEqConstraint(lhv, rhv)
            else -> UtRefEqConstraint(lhv, rhv)
        }
    }

    override fun visit(expr: UtBoolConst): UtConstraint = applyConstraint(expr) {
        UtBoolConstraint(expr.accept(varBuilder))
    }!!

    override fun visit(expr: NotBoolExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtOrBoolExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtAndBoolExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtNegExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtCastExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtBoolOpExpression): UtConstraint? = applyConstraint(expr) {
        if (shouldSkip(expr)) return@applyConstraint null
        val lhv = expr.left.expr.accept(varBuilder)
        val rhv = expr.right.expr.accept(varBuilder)
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
                false -> UtRefEqConstraint(lhv, rhv)
            }
            Ne -> when (lhv.isPrimitive && rhv.isPrimitive) {
                true -> UtNeqConstraint(lhv, rhv)
                false -> UtRefNeqConstraint(lhv, rhv)
            }
        }
    }

    override fun visit(expr: UtIsExpression): UtConstraint? {
        if (shouldSkip(expr)) return null
        val operand = expr.addr.accept(varBuilder)
        return UtRefTypeConstraint(operand, expr.type.classId)
    }

    override fun visit(expr: UtGenericExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtIsGenericTypeExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtInstanceOfExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtIteExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtMkTermArrayExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringConst): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtConcatExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtConvertToString): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringToInt): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringLength): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringPositiveLength): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringCharAt): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringEq): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtSubstringExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtReplaceExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStartsWithExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtEndsWithExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtIndexOfExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtContainsExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtToStringExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtSeqLiteral): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayToString): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayInsert): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayInsertRange): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayRemove): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayRemoveRange): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArraySetRange): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayShiftIndexes): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtArrayApplyForAll): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtStringToArray): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtAddNoOverflowExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

    override fun visit(expr: UtSubNoOverflowExpression): UtConstraint {
        return UtEqConstraint(UtConstraintBoolConstant(true), UtConstraintBoolConstant(true))
    }

}