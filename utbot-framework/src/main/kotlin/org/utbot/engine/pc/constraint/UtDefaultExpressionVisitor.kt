package org.utbot.engine.pc.constraint

import org.utbot.engine.pc.*

open class UtDefaultExpressionVisitor<TResult>(
    val default: () -> TResult
) : UtExpressionVisitor<TResult> {
    override fun visit(expr: UtArraySelectExpression): TResult = default()
    override fun visit(expr: UtConstArrayExpression): TResult = default()
    override fun visit(expr: UtMkArrayExpression): TResult = default()
    override fun visit(expr: UtArrayMultiStoreExpression): TResult = default()
    override fun visit(expr: UtBvLiteral): TResult = default()
    override fun visit(expr: UtBvConst): TResult = default()
    override fun visit(expr: UtAddrExpression): TResult = default()
    override fun visit(expr: UtFpLiteral): TResult = default()
    override fun visit(expr: UtFpConst): TResult = default()
    override fun visit(expr: UtOpExpression): TResult = default()
    override fun visit(expr: UtTrue): TResult = default()
    override fun visit(expr: UtFalse): TResult = default()
    override fun visit(expr: UtEqExpression): TResult = default()
    override fun visit(expr: UtBoolConst): TResult = default()
    override fun visit(expr: NotBoolExpression): TResult = default()
    override fun visit(expr: UtOrBoolExpression): TResult = default()
    override fun visit(expr: UtAndBoolExpression): TResult = default()
    override fun visit(expr: UtNegExpression): TResult = default()
    override fun visit(expr: UtCastExpression): TResult = default()
    override fun visit(expr: UtBoolOpExpression): TResult = default()
    override fun visit(expr: UtIsExpression): TResult = default()
    override fun visit(expr: UtGenericExpression): TResult = default()
    override fun visit(expr: UtIsGenericTypeExpression): TResult = default()
    override fun visit(expr: UtEqGenericTypeParametersExpression): TResult = default()
    override fun visit(expr: UtInstanceOfExpression): TResult = default()
    override fun visit(expr: UtIteExpression): TResult = default()
    override fun visit(expr: UtMkTermArrayExpression): TResult = default()

    // UtString expressions
    override fun visit(expr: UtStringConst): TResult = default()
    override fun visit(expr: UtConcatExpression): TResult = default()
    override fun visit(expr: UtConvertToString): TResult = default()
    override fun visit(expr: UtStringToInt): TResult = default()
    override fun visit(expr: UtStringLength): TResult = default()
    override fun visit(expr: UtStringPositiveLength): TResult = default()
    override fun visit(expr: UtStringCharAt): TResult = default()
    override fun visit(expr: UtStringEq): TResult = default()
    override fun visit(expr: UtSubstringExpression): TResult = default()
    override fun visit(expr: UtReplaceExpression): TResult = default()
    override fun visit(expr: UtStartsWithExpression): TResult = default()
    override fun visit(expr: UtEndsWithExpression): TResult = default()
    override fun visit(expr: UtIndexOfExpression): TResult = default()
    override fun visit(expr: UtContainsExpression): TResult = default()
    override fun visit(expr: UtToStringExpression): TResult = default()
    override fun visit(expr: UtSeqLiteral): TResult = default()
    override fun visit(expr: UtArrayToString): TResult = default()

    // UtArray expressions from extended array theory
    override fun visit(expr: UtArrayInsert): TResult = default()
    override fun visit(expr: UtArrayInsertRange): TResult = default()
    override fun visit(expr: UtArrayRemove): TResult = default()
    override fun visit(expr: UtArrayRemoveRange): TResult = default()
    override fun visit(expr: UtArraySetRange): TResult = default()
    override fun visit(expr: UtArrayShiftIndexes): TResult = default()
    override fun visit(expr: UtArrayApplyForAll): TResult = default()
    override fun visit(expr: UtStringToArray): TResult = default()

    // Add and Sub with overflow detection
    override fun visit(expr: UtAddNoOverflowExpression): TResult = default()
    override fun visit(expr: UtSubNoOverflowExpression): TResult = default()
}