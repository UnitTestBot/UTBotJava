package org.utbot.engine.pc

class UtAtomCollector(val predicate: (UtExpression) -> Boolean) : UtExpressionVisitor<Set<UtExpression>> {
    val result = mutableSetOf<UtExpression>()
    private var currentParent: UtExpression? = null

    private inline fun <reified T : UtExpression> visitBooleanExpr(expr: T, visitor: (T) -> Unit): Set<UtExpression> {
        val oldParent = currentParent
        if (expr.sort is UtBoolSort) {
            currentParent = expr
        }
        if (predicate(expr)) {
            result += currentParent!!
        } else {
            visitor(expr)
        }
        currentParent = oldParent
        return result
    }

    override fun visit(expr: UtArraySelectExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.arrayExpression.accept(this)
        it.index.accept(this)
    }

    override fun visit(expr: UtConstArrayExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.constValue.accept(this)
    }

    override fun visit(expr: UtMkArrayExpression): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtArrayMultiStoreExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.initial.accept(this)
        it.stores.forEach { store ->
            store.index.accept(this)
            store.value.accept(this)
        }
    }

    override fun visit(expr: UtBvLiteral): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtBvConst): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtAddrExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.internal.accept(this)
    }

    override fun visit(expr: UtFpLiteral): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtFpConst): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtOpExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.left.expr.accept(this)
        it.right.expr.accept(this)
    }

    override fun visit(expr: UtTrue): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtFalse): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtEqExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.left.accept(this)
        it.right.accept(this)
    }

    override fun visit(expr: UtBoolConst): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: NotBoolExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.expr.accept(this)
    }

    override fun visit(expr: UtOrBoolExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.exprs.forEach { operand -> operand.accept(this) }
    }

    override fun visit(expr: UtAndBoolExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.exprs.forEach { operand -> operand.accept(this) }
    }

    override fun visit(expr: UtNegExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.variable.expr.accept(this)
    }

    override fun visit(expr: UtCastExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.variable.expr.accept(this)
    }

    override fun visit(expr: UtBoolOpExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.left.expr.accept(this)
        it.right.expr.accept(this)
    }

    override fun visit(expr: UtIsExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.addr.accept(this)
    }

    override fun visit(expr: UtGenericExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.addr.accept(this)
    }

    override fun visit(expr: UtIsGenericTypeExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.addr.accept(this)
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.firstAddr.accept(this)
        it.secondAddr.accept(this)
    }

    override fun visit(expr: UtInstanceOfExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.constraint.accept(this)
    }

    override fun visit(expr: UtIteExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.condition.accept(this)
        it.thenExpr.accept(this)
        it.elseExpr.accept(this)
    }

    override fun visit(expr: UtMkTermArrayExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.array.accept(this)
        it.defaultValue?.accept(this)
    }

    override fun visit(expr: UtStringConst): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtConcatExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        it.parts.forEach { part -> part.accept(this) }
    }

    override fun visit(expr: UtConvertToString): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.expression.accept(this)
    }

    override fun visit(expr: UtStringToInt): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.expression.accept(this)
    }

    override fun visit(expr: UtStringLength): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
    }

    override fun visit(expr: UtStringPositiveLength): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
    }

    override fun visit(expr: UtStringCharAt): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.index.accept(this)
    }

    override fun visit(expr: UtStringEq): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visit(expr: UtSubstringExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.beginIndex.accept(this)
        expr.length.accept(this)
    }

    override fun visit(expr: UtReplaceExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.regex.accept(this)
        expr.replacement.accept(this)
    }

    override fun visit(expr: UtStartsWithExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.prefix.accept(this)
    }

    override fun visit(expr: UtEndsWithExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.suffix.accept(this)
    }

    override fun visit(expr: UtIndexOfExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.substring.accept(this)
    }

    override fun visit(expr: UtContainsExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.string.accept(this)
        expr.substring.accept(this)
    }

    override fun visit(expr: UtToStringExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.notNullExpr.accept(this)
        expr.isNull.accept(this)
    }

    override fun visit(expr: UtSeqLiteral): Set<UtExpression> = visitBooleanExpr(expr) {}

    override fun visit(expr: UtArrayToString): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayInsert): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayInsertRange): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayRemove): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayRemoveRange): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArraySetRange): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayShiftIndexes): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayApplyForAll): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtStringToArray): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.stringExpression.accept(this)
        expr.offset.expr.accept(this)
    }

    override fun visit(expr: UtAddNoOverflowExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visit(expr: UtSubNoOverflowExpression): Set<UtExpression> = visitBooleanExpr(expr) {
        expr.left.accept(this)
        expr.right.accept(this)
    }
}