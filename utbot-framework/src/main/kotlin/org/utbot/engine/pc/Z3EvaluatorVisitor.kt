package org.utbot.engine.pc

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.MAX_STRING_LENGTH_SIZE_BITS
import org.utbot.engine.z3.convertVar
import org.utbot.engine.z3.intValue
import org.utbot.engine.z3.negate
import org.utbot.engine.z3.value
import com.microsoft.z3.ArrayExpr
import com.microsoft.z3.ArraySort
import com.microsoft.z3.BitVecExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Expr
import com.microsoft.z3.Model
import com.microsoft.z3.SeqExpr
import com.microsoft.z3.eval
import com.microsoft.z3.mkSeqNth
import soot.ByteType
import soot.CharType

class Z3EvaluatorVisitor(private val model: Model, private val translator: Z3TranslatorVisitor) :
    UtExpressionVisitor<Expr> by translator {

    // stack of indexes that are visited in expression in derivation tree.
    // For example, we call eval(select(select(a, i), j)
    // On visiting term a, the stack will be equals to [i, j]
    private val selectIndexStack = mutableListOf<Expr>()

    private inline fun withPushedSelectIndex(index: UtExpression, block: () -> Expr): Expr {
        selectIndexStack += eval(index)
        return block().also {
            selectIndexStack.removeLast()
        }
    }

    private inline fun withPoppedSelectIndex(block: () -> Expr): Expr {
        val lastIndex = selectIndexStack.removeLast()
        return block().also {
            selectIndexStack += lastIndex
        }
    }

    private fun foldSelectsFromStack(expr: Expr): Expr {
        var result = expr
        var i = selectIndexStack.size
        while (i > 0 && result.sort is ArraySort) {
            result = translator.withContext { mkSelect(result as ArrayExpr, selectIndexStack[--i]) }
        }
        return result
    }


    fun eval(expr: UtExpression): Expr {
        val translated = if (expr.sort is UtArraySort) {
            translator.lookUpCache(expr)?.let { foldSelectsFromStack(it) } ?: expr.accept(this)
        } else {
            translator.lookUpCache(expr) ?: expr.accept(this)
        }
        return model.eval(translated)
    }

    override fun visit(expr: UtArraySelectExpression): Expr = expr.run {
        // translate arrayExpression here will return evaluation of arrayExpression.select(index)
        withPushedSelectIndex(index) {
            eval(arrayExpression)
        }
    }

    override fun visit(expr: UtConstArrayExpression): Expr = expr.run {
        // expr.select(index) = constValue for any index
        if (selectIndexStack.size == 0) {
            translator.withContext { mkConstArray(sort.indexSort.toZ3Sort(this), eval(constValue)) }
        } else {
            withPoppedSelectIndex {
                eval(constValue)
            }
        }
    }

    override fun visit(expr: UtMkArrayExpression): Expr =
    // mkArray expression can have more than one dimension
    // so for such case select(select(mkArray(...), i), j))
        // indices i and j are currently in the stack and we need to fold them.
        foldSelectsFromStack(translator.translate(expr))

    override fun visit(expr: UtArrayMultiStoreExpression): Expr = expr.run {
        val lastIndex = selectIndexStack.lastOrNull()
            ?: return translator.withContext {
                stores.fold(translator.translate(initial) as ArrayExpr) { acc, (index, item) ->
                    mkStore(acc, eval(index), eval(item))
                }
            }
        for (store in stores.asReversed()) {
            if (lastIndex == eval(store.index)) {
                return withPoppedSelectIndex { eval(store.value) }
            }
        }
        eval(initial)
    }

    override fun visit(expr: UtMkTermArrayExpression): Expr = expr.run {
        // should we make eval(mkTerm) always true??
        translator.translate(UtTrue)
    }

    override fun visit(expr: UtArrayInsert): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val index = eval(index.expr).intValue()
        when {
            lastIndex == index -> eval(element)
            lastIndex < index -> eval(arrayExpression.select(mkInt(lastIndex)))
            else -> eval(arrayExpression.select(mkInt(lastIndex - 1)))
        }
    }

    override fun visit(expr: UtArrayInsertRange): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val index = eval(index.expr).intValue()
        val length = eval(length.expr).intValue()
        when {
            lastIndex < index -> eval(arrayExpression.select(mkInt(lastIndex)))
            lastIndex >= index + length -> eval(arrayExpression.select(mkInt(lastIndex - length)))
            else -> {
                val from = eval(from.expr).intValue()
                eval(elements.select(mkInt(lastIndex - index + from)))
            }
        }
    }

    override fun visit(expr: UtArrayRemove): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val index = eval(index.expr).intValue()
        if (lastIndex < index) {
            eval(arrayExpression.select(mkInt(lastIndex)))
        } else {
            eval(arrayExpression.select(mkInt(lastIndex + 1)))
        }
    }

    override fun visit(expr: UtArrayRemoveRange): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val index = eval(index.expr).intValue()
        if (lastIndex < index) {
            eval(arrayExpression.select(mkInt(lastIndex)))
        } else {
            val length = eval(length.expr).intValue()
            eval(arrayExpression.select(mkInt(lastIndex + length)))
        }
    }

    override fun visit(expr: UtArraySetRange): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val index = eval(index.expr).intValue()
        val length = eval(length.expr).intValue()
        when {
            lastIndex < index || lastIndex >= index + length -> eval(arrayExpression.select(mkInt(lastIndex)))
            else -> {
                val from = eval(from.expr).intValue()
                eval(elements.select(mkInt(lastIndex - index + from)))
            }
        }
    }

    override fun visit(expr: UtArrayShiftIndexes): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val offset = eval(offset.expr).intValue()
        eval(arrayExpression.select(mkInt(lastIndex - offset)))
    }

    override fun visit(expr: UtStringToArray): Expr = expr.run {
        val lastIndex = selectIndexStack.last().intValue()
        val offset = eval(offset.expr).intValue()
        eval(UtStringCharAt(stringExpression, mkInt(lastIndex + offset)))
    }

    override fun visit(expr: UtAddrExpression): Expr = eval(expr.internal)

    override fun visit(expr: UtOpExpression): Expr = expr.run {
        val leftResolve = eval(left.expr).z3Variable(left.type)
        val rightResolve = eval(right.expr).z3Variable(right.type)
        translator.withContext {
            operator.delegate(this, leftResolve, rightResolve)
        }
    }

    override fun visit(expr: UtEqExpression): Expr = expr.run {
        translator.withContext { mkEq(eval(left), eval(right)) }
    }

    override fun visit(expr: NotBoolExpression): Expr =
        translator.withContext { mkNot(eval(expr.expr) as BoolExpr) }

    override fun visit(expr: UtOrBoolExpression): Expr = expr.run {
        translator.withContext {
            mkOr(*expr.exprs.map { eval(it) as BoolExpr }.toTypedArray())
        }
    }

    override fun visit(expr: UtAndBoolExpression): Expr = expr.run {
        translator.withContext {
            mkAnd(*expr.exprs.map { eval(it) as BoolExpr }.toTypedArray())
        }
    }

    override fun visit(expr: UtAddNoOverflowExpression): Expr = expr.run {
        translator.withContext {
            mkBVAddNoOverflow(eval(expr.left) as BitVecExpr, eval(expr.right) as BitVecExpr, true)
        }
    }

    override fun visit(expr: UtSubNoOverflowExpression): Expr = expr.run {
        translator.withContext {
            // For some reason mkBVSubNoOverflow does not take "signed" as an argument, yet still works for signed integers
            mkBVSubNoOverflow(eval(expr.left) as BitVecExpr, eval(expr.right) as BitVecExpr) //, true)
        }
    }

    override fun visit(expr: UtNegExpression): Expr = expr.run {
        translator.withContext {
            negate(this, eval(variable.expr).z3Variable(variable.type))
        }
    }

    override fun visit(expr: UtCastExpression): Expr = expr.run {
        val z3var = eval(variable.expr).z3Variable(variable.type)
        translator.withContext { convertVar(z3var, type).expr }
    }

    override fun visit(expr: UtBoolOpExpression): Expr = expr.run {
        val leftResolve = eval(left.expr).z3Variable(left.type)
        val rightResolve = eval(right.expr).z3Variable(right.type)
        translator.withContext {
            operator.delegate(this, leftResolve, rightResolve)
        }
    }

    override fun visit(expr: UtIsExpression): Expr = translator.translate(expr)

    override fun visit(expr: UtInstanceOfExpression): Expr =
        expr.run { eval(expr.constraint) }

    override fun visit(expr: UtIteExpression): Expr = expr.run {
        if (eval(condition).value() as Boolean) eval(thenExpr) else eval(elseExpr)
    }

    override fun visit(expr: UtConcatExpression): Expr = expr.run {
        translator.withContext { mkConcat(*parts.map { eval(it) as SeqExpr }.toTypedArray()) }
    }

    override fun visit(expr: UtStringLength): Expr = expr.run {
        translator.withContext {
            if (string is UtArrayToString) {
                eval(string.length.expr)
            } else {
                mkInt2BV(MAX_STRING_LENGTH_SIZE_BITS, mkLength(eval(string) as SeqExpr))
            }
        }
    }

    override fun visit(expr: UtStringPositiveLength): Expr = expr.run {
        translator.withContext {
            mkGe(mkLength(eval(string) as SeqExpr), mkInt(0))
        }
    }

    override fun visit(expr: UtStringCharAt): Expr = expr.run {
        translator.withContext {
            val charAtExpr = mkSeqNth(eval(string) as SeqExpr, mkBV2Int(eval(index) as BitVecExpr, true))
            val z3var = charAtExpr.z3Variable(ByteType.v())
            convertVar(z3var, CharType.v()).expr
        }
    }

    override fun visit(expr: UtStringEq): Expr = expr.run {
        translator.withContext {
            mkEq(eval(left), eval(right))
        }
    }

    override fun visit(expr: UtSubstringExpression): Expr = expr.run {
        translator.withContext {
            mkExtract(
                eval(string) as SeqExpr,
                mkBV2Int(eval(beginIndex) as BitVecExpr, true),
                mkBV2Int(eval(length) as BitVecExpr, true)
            )
        }
    }

    override fun visit(expr: UtReplaceExpression): Expr = expr.run {
        workaround(WorkaroundReason.HACK) { // mkReplace replaces first occasion only
            translator.withContext {
                mkReplace(
                    eval(string) as SeqExpr,
                    eval(regex) as SeqExpr,
                    eval(replacement) as SeqExpr
                )
            }
        }
    }

    // Attention, prefix is a first argument!
    override fun visit(expr: UtStartsWithExpression): Expr = expr.run {
        translator.withContext {
            mkPrefixOf(eval(prefix) as SeqExpr, eval(string) as SeqExpr)
        }
    }

    // Attention, suffix is a first argument!
    override fun visit(expr: UtEndsWithExpression): Expr = expr.run {
        translator.withContext {
            mkSuffixOf(eval(suffix) as SeqExpr, eval(string) as SeqExpr)
        }
    }

    override fun visit(expr: UtIndexOfExpression): Expr = expr.run {
        val string = eval(string) as SeqExpr
        val substring = eval(substring) as SeqExpr
        translator.withContext {
            mkInt2BV(
                MAX_STRING_LENGTH_SIZE_BITS,
                mkIndexOf(string, substring, mkInt(0))
            )
        }
    }

    override fun visit(expr: UtContainsExpression): Expr = expr.run {
        val substring = eval(substring) as SeqExpr
        val string = eval(string) as SeqExpr
        translator.withContext {
            mkGe(mkIndexOf(string, substring, mkInt(0)), mkInt(0))
        }
    }

    override fun visit(expr: UtToStringExpression): Expr = expr.run {
        if (eval(isNull).value() as Boolean) translator.withContext { mkString("null") } else eval(notNullExpr)
    }

    override fun visit(expr: UtArrayApplyForAll): Expr = expr.run {
        eval(expr.arrayExpression.select(mkInt(selectIndexStack.last().intValue())))
    }
}
