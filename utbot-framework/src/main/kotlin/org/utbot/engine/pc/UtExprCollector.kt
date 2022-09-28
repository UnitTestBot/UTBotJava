package org.utbot.engine.pc

class UtExprCollector(val predicate: (UtExpression) -> Boolean) : UtExpressionVisitor<Set<UtExpression>> {
    val results = mutableSetOf<UtExpression>()

    private inline fun <reified T : UtExpression> visitExpr(expr: T, body: (T) -> Unit): Set<UtExpression> {
        if (predicate(expr)) {
            results += expr
        }
        body(expr)
        return results
    }

    override fun visit(expr: UtArraySelectExpression): Set<UtExpression> = visitExpr(expr) {
        it.arrayExpression.accept(this)
        it.index.accept(this)
    }

    override fun visit(expr: UtConstArrayExpression): Set<UtExpression> = visitExpr(expr) {
        it.constValue.accept(this)
    }

    override fun visit(expr: UtMkArrayExpression): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtArrayMultiStoreExpression): Set<UtExpression> = visitExpr(expr) {
        it.initial.accept(this)
        it.stores.forEach { store ->
            store.index.accept(this)
            store.value.accept(this)
        }
    }

    override fun visit(expr: UtBvLiteral): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtBvConst): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtAddrExpression): Set<UtExpression> = visitExpr(expr) {
        it.internal.accept(this)
    }

    override fun visit(expr: UtFpLiteral): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtFpConst): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtOpExpression): Set<UtExpression> = visitExpr(expr) {
        it.left.expr.accept(this)
        it.right.expr.accept(this)
    }

    override fun visit(expr: UtTrue): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtFalse): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtEqExpression): Set<UtExpression> = visitExpr(expr) {
        it.left.accept(this)
        it.right.accept(this)
    }

    override fun visit(expr: UtBoolConst): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: NotBoolExpression): Set<UtExpression> = visitExpr(expr) {
        it.expr.accept(this)
    }

    override fun visit(expr: UtOrBoolExpression): Set<UtExpression> = visitExpr(expr) {
        it.exprs.forEach { operand -> operand.accept(this) }
    }

    override fun visit(expr: UtAndBoolExpression): Set<UtExpression> = visitExpr(expr) {
        it.exprs.forEach { operand -> operand.accept(this) }
    }

    override fun visit(expr: UtNegExpression): Set<UtExpression> = visitExpr(expr) {
        it.variable.expr.accept(this)
    }

    override fun visit(expr: UtCastExpression): Set<UtExpression> = visitExpr(expr) {
        it.variable.expr.accept(this)
    }

    override fun visit(expr: UtBoolOpExpression): Set<UtExpression> = visitExpr(expr) {
        it.left.expr.accept(this)
        it.right.expr.accept(this)
    }

    override fun visit(expr: UtIsExpression): Set<UtExpression> = visitExpr(expr) {
        it.addr.accept(this)
    }

    override fun visit(expr: UtGenericExpression): Set<UtExpression> = visitExpr(expr) {
        it.addr.accept(this)
    }

    override fun visit(expr: UtIsGenericTypeExpression): Set<UtExpression> = visitExpr(expr) {
        it.addr.accept(this)
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): Set<UtExpression> = visitExpr(expr) {
        it.firstAddr.accept(this)
        it.secondAddr.accept(this)
    }

    override fun visit(expr: UtInstanceOfExpression): Set<UtExpression> = visitExpr(expr) {
        it.constraint.accept(this)
    }

    override fun visit(expr: UtIteExpression): Set<UtExpression> = visitExpr(expr) {
        it.condition.accept(this)
        it.thenExpr.accept(this)
        it.elseExpr.accept(this)
    }

    override fun visit(expr: UtMkTermArrayExpression): Set<UtExpression> = visitExpr(expr) {
        it.array.accept(this)
        it.defaultValue?.accept(this)
    }

    override fun visit(expr: UtStringConst): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtConcatExpression): Set<UtExpression> = visitExpr(expr) {
        it.parts.forEach { part -> part.accept(this) }
    }

    override fun visit(expr: UtConvertToString): Set<UtExpression> = visitExpr(expr) {
        expr.expression.accept(this)
    }

    override fun visit(expr: UtStringToInt): Set<UtExpression> = visitExpr(expr) {
        expr.expression.accept(this)
    }

    override fun visit(expr: UtStringLength): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
    }

    override fun visit(expr: UtStringPositiveLength): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
    }

    override fun visit(expr: UtStringCharAt): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.index.accept(this)
    }

    override fun visit(expr: UtStringEq): Set<UtExpression> = visitExpr(expr) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visit(expr: UtSubstringExpression): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.beginIndex.accept(this)
        expr.length.accept(this)
    }

    override fun visit(expr: UtReplaceExpression): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.regex.accept(this)
        expr.replacement.accept(this)
    }

    override fun visit(expr: UtStartsWithExpression): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.prefix.accept(this)
    }

    override fun visit(expr: UtEndsWithExpression): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.suffix.accept(this)
    }

    override fun visit(expr: UtIndexOfExpression): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.substring.accept(this)
    }

    override fun visit(expr: UtContainsExpression): Set<UtExpression> = visitExpr(expr) {
        expr.string.accept(this)
        expr.substring.accept(this)
    }

    override fun visit(expr: UtToStringExpression): Set<UtExpression> = visitExpr(expr) {
        expr.notNullExpr.accept(this)
        expr.isNull.accept(this)
    }

    override fun visit(expr: UtSeqLiteral): Set<UtExpression> = visitExpr(expr) {}

    override fun visit(expr: UtArrayToString): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayInsert): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayInsertRange): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayRemove): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayRemoveRange): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArraySetRange): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayShiftIndexes): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtArrayApplyForAll): Set<UtExpression> = visitExpr(expr) {
        expr.arrayExpression.accept(this)
    }

    override fun visit(expr: UtStringToArray): Set<UtExpression> = visitExpr(expr) {
        expr.stringExpression.accept(this)
        expr.offset.expr.accept(this)
    }

    override fun visit(expr: UtAddNoOverflowExpression): Set<UtExpression> = visitExpr(expr) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visit(expr: UtSubNoOverflowExpression): Set<UtExpression> = visitExpr(expr) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

}