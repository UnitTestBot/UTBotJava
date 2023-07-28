package org.utbot.engine.pc

interface UtExpressionVisitor<TResult> {
    fun visit(expr: UtArraySelectExpression): TResult
    fun visit(expr: UtConstArrayExpression): TResult
    fun visit(expr: UtMkArrayExpression): TResult
    fun visit(expr: UtArrayMultiStoreExpression): TResult
    fun visit(expr: UtBvLiteral): TResult
    fun visit(expr: UtBvConst): TResult
    fun visit(expr: UtAddrExpression): TResult
    fun visit(expr: UtFpLiteral): TResult
    fun visit(expr: UtFpConst): TResult
    fun visit(expr: UtOpExpression): TResult
    fun visit(expr: UtTrue): TResult
    fun visit(expr: UtFalse): TResult
    fun visit(expr: UtEqExpression): TResult
    fun visit(expr: UtBoolConst): TResult
    fun visit(expr: NotBoolExpression): TResult
    fun visit(expr: UtOrBoolExpression): TResult
    fun visit(expr: UtAndBoolExpression): TResult
    fun visit(expr: UtNegExpression): TResult
    fun visit(expr: UtBvNotExpression): TResult
    fun visit(expr: UtCastExpression): TResult
    fun visit(expr: UtBoolOpExpression): TResult
    fun visit(expr: UtIsExpression): TResult
    fun visit(expr: UtGenericExpression): TResult
    fun visit(expr: UtIsGenericTypeExpression): TResult
    fun visit(expr: UtEqGenericTypeParametersExpression): TResult
    fun visit(expr: UtInstanceOfExpression): TResult
    fun visit(expr: UtIteExpression): TResult
    fun visit(expr: UtMkTermArrayExpression): TResult

    // UtArray expressions from extended array theory
    fun visit(expr: UtArrayInsert): TResult
    fun visit(expr: UtArrayInsertRange): TResult
    fun visit(expr: UtArrayRemove): TResult
    fun visit(expr: UtArrayRemoveRange): TResult
    fun visit(expr: UtArraySetRange): TResult
    fun visit(expr: UtArrayShiftIndexes): TResult
    fun visit(expr: UtArrayApplyForAll): TResult

    // Add and Sub with overflow detection
    fun visit(expr: UtAddNoOverflowExpression): TResult
    fun visit(expr: UtSubNoOverflowExpression): TResult
    fun visit(expr: UtMulNoOverflowExpression): TResult
}