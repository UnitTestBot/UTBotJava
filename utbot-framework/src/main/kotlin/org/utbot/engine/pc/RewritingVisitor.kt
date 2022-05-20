package org.utbot.engine.pc

import org.utbot.common.unreachableBranch
import org.utbot.engine.Add
import org.utbot.engine.And
import org.utbot.engine.Cmp
import org.utbot.engine.Cmpg
import org.utbot.engine.Cmpl
import org.utbot.engine.Div
import org.utbot.engine.Eq
import org.utbot.engine.Ge
import org.utbot.engine.Gt
import org.utbot.engine.Le
import org.utbot.engine.Lt
import org.utbot.engine.Mul
import org.utbot.engine.Ne
import org.utbot.engine.Or
import org.utbot.engine.PrimitiveValue
import org.utbot.engine.Rem
import org.utbot.engine.Shl
import org.utbot.engine.Shr
import org.utbot.engine.Sub
import org.utbot.engine.Ushr
import org.utbot.engine.Xor
import org.utbot.engine.maxSort
import org.utbot.engine.primitiveToLiteral
import org.utbot.engine.primitiveToSymbolic
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.toIntValue
import org.utbot.engine.toPrimitiveValue
import org.utbot.framework.UtSettings.useExpressionSimplification
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.collections.immutable.toPersistentList
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.IntegerType
import soot.LongType
import soot.Type


/**
 * UtExpressionVisitor that performs simple rewritings on expressions with concrete values.
 *
 */
open class RewritingVisitor(
    private val eqs: Map<UtExpression, UtExpression> = emptyMap(),
    private val lts: Map<UtExpression, Long> = emptyMap(),
    private val gts: Map<UtExpression, Long> = emptyMap()
) : UtExpressionVisitor<UtExpression> {
    val axiomInstantiationVisitor: RewritingVisitor
        get() = AxiomInstantiationRewritingVisitor(eqs, lts, gts)
    protected val selectIndexStack = mutableListOf<PrimitiveValue>()
    private val simplificationCache = IdentityHashMap<UtExpression, UtExpression>()

    private fun allConcrete(vararg exprs: UtExpression) = exprs.all { it.isConcrete }

    protected inline fun <reified R> withNewSelect(index: UtExpression, block: () -> R): R {
        selectIndexStack += index.toIntValue()
        return block().also {
            selectIndexStack.removeLast()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected inline fun <reified R> withEmptySelects(block: () -> R): R {
        val currentSelectStack = selectIndexStack.toList()
        selectIndexStack.clear()
        return block().also {
            selectIndexStack.addAll(currentSelectStack)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected inline fun <reified R> withRemoveSelect(block: () -> R): R {
        val lastIndex = selectIndexStack.removeLastOrNull()
        return block().also {
            if (lastIndex != null) {
                selectIndexStack += lastIndex
            }
        }
    }

    /**
     * if [useExpressionSimplification] is true then rewrite [expr] and
     * substitute concrete values from [eqs] map for equal symbolic expressions.
     *
     * @param expr - simplified expression
     * @param block - simplification function. Identity function by default
     * @param substituteResult - if true then try to substitute result of block(expr)
     *  with equal concrete value from [eqs] map.
     */
    private inline fun applySimplification(
        expr: UtExpression,
        substituteResult: Boolean = true,
        crossinline block: (() -> UtExpression) = { expr },
    ): UtExpression {
        val value = {
            if (!useExpressionSimplification) {
                expr
            } else {
                eqs[expr] ?: block().let { if (substituteResult) eqs[it] ?: it else it }
            }
        }
        return if (expr.sort is UtArraySort && selectIndexStack.isNotEmpty()) {
            checkSortsEquality(value, expr)
        } else {
            simplificationCache.getOrPut(expr) {
                checkSortsEquality(value, expr)
            }
        }
    }

    private fun checkSortsEquality(value: () -> UtExpression, sourceExpr: UtExpression): UtExpression {
        val result = value()

        val sourceSort = sourceExpr.sort
        val resultSort = result.sort

        // We should return simplified expression with the same sort as the original one.
        // The only exception is a case with two primitive sorts and cast expression below.
        // So, expression with, e.g., UtArraySort must be returned here.
        if (resultSort == sourceSort) return result

        // There we must have the following situation: original expression is `x = Add(a, b)`, where
        // x :: Int32, a = 0 :: Int32, b :: Short16. Simplifier removes the left part of the addition since it is
        // equal to zero, now we have just b with a different from the original expression sort.
        // We must cast it to the required sort, so the result will be `Cast(b, Int32)`.
        require(sourceSort is UtPrimitiveSort && resultSort is UtPrimitiveSort) {
            val messageParts = listOf(
                "Expr to simplify had sort ${sourceSort}:", sourceExpr, "",
                "After simplification expression got sort ${resultSort}:", result
            )
            messageParts.joinToString(System.lineSeparator(), prefix = System.lineSeparator())
        }

        val expectedType = sourceSort.type
        val resultType = resultSort.type

        return UtCastExpression(result.toPrimitiveValue(resultType), expectedType)
    }

    override fun visit(expr: UtArraySelectExpression): UtExpression =
        applySimplification(expr) {
            val index = withEmptySelects { expr.index.accept(this) }
            when (val array = withNewSelect(index) { expr.arrayExpression.accept(this) }) {
                // select(constArray(a), i) --> a
                is UtConstArrayExpression -> array.constValue
                is UtArrayMultiStoreExpression -> {
                    // select(store(..(store(array, Concrete a, b), Concrete x, _)..), Concrete a) -> b
                    val newStores = array.stores.filter { UtEqExpression(index, it.index).accept(this) != UtFalse }

                    when {
                        newStores.isEmpty() -> array.initial.select(index).accept(this)
                        UtEqExpression(index, newStores.last().index).accept(this) == UtTrue -> newStores.last().value
                        else -> UtArrayMultiStoreExpression(array.initial, newStores.toPersistentList()).select(index)
                    }
                }
                else -> array.select(index)
            }
        }

    override fun visit(expr: UtMkArrayExpression): UtExpression = expr

    // store(store(store(array, 1, b), 2, a), 1, a) ---> store(store(array, 2, a), 1, a)
    override fun visit(expr: UtArrayMultiStoreExpression): UtExpression =
        applySimplification(expr) {
            val initial = expr.initial.accept(this)
            val stores = withRemoveSelect {
                expr.stores.map {
                    val index = it.index.accept(this)
                    val value = it.value.accept(this)

                    require(value.sort == expr.sort.itemSort) {
                        "Unequal sorts occurred during rewriting UtArrayMultiStoreExpression:\n" +
                                "value with index $index has sort ${value.sort} while " +
                                "${expr.sort.itemSort} was expected."
                    }

                    UtStore(index, value)
                }
            }.filterNot { it.value == initial.select(it.index) }
            when {
                stores.isEmpty() -> initial
                initial is UtArrayMultiStoreExpression -> UtArrayMultiStoreExpression(
                    initial.initial,
                    (initial.stores + stores).asReversed().distinctBy { it.index }.asReversed().toPersistentList()
                )
                else -> UtArrayMultiStoreExpression(
                    initial,
                    stores.asReversed().distinctBy { it.index }.asReversed().toPersistentList()
                )
            }
        }

    override fun visit(expr: UtBvLiteral): UtExpression = expr

    override fun visit(expr: UtBvConst): UtExpression = applySimplification(expr, true)

    override fun visit(expr: UtAddrExpression): UtExpression =
        applySimplification(expr) {
            UtAddrExpression(
                expr.internal.accept(this)
            )
        }

    override fun visit(expr: UtFpLiteral): UtExpression = expr

    override fun visit(expr: UtFpConst): UtExpression = applySimplification(expr)

    private fun evalIntegralLiterals(literal: UtExpression, sort: UtSort, block: (Long) -> Number): UtExpression =
        block(literal.toLong()).toValueWithSort(sort).primitiveToLiteral()

    private fun evalIntegralLiterals(
        left: UtExpression,
        right: UtExpression,
        sort: UtSort,
        block: (Long, Long) -> Number
    ): UtExpression =
        block(left.toLong(), right.toLong()).toValueWithSort(sort).primitiveToLiteral()

    private fun evalIntegralLiterals(
        left: UtExpression,
        right: UtExpression,
        block: (Long, Long) -> Boolean
    ): UtExpression =
        block(left.toLong(), right.toLong()).primitiveToLiteral()

    private fun Number.toValueWithSort(sort: UtSort): Any =
        when (sort) {
            UtBoolSort -> this != UtLongFalse
            UtByteSort -> this.toByte()
            UtShortSort -> this.toShort()
            UtCharSort -> this.toChar()
            UtIntSort -> this.toInt()
            UtLongSort -> this.toLong()
            UtFp32Sort -> this.toFloat()
            UtFp64Sort -> this.toDouble()
            else -> error("Can convert Number values only to primitive sorts. Sort $sort isn't primitive")
        }

    private fun UtBoolLiteral.toLong(): Long = if (value) UtLongTrue else UtLongFalse

    private fun UtBvLiteral.toLong(): Long = value.toLong()

    private fun UtExpression.toLong(): Long = when (this) {
        is UtBvLiteral -> toLong()
        is UtBoolLiteral -> toLong()
        is UtAddrExpression -> internal.toLong()
        else -> error("$this isn't IntegralLiteral")
    }

    private val UtExpression.isIntegralLiteral: Boolean
        get() = when (this) {
            is UtBvLiteral -> true
            is UtBoolLiteral -> true
            is UtAddrExpression -> internal.isIntegralLiteral
            else -> false
        }

    override fun visit(expr: UtOpExpression): UtExpression =
        applySimplification(expr) {
            val left = expr.left.expr.accept(this)
            val right = expr.right.expr.accept(this)
            val leftPrimitive = left.toPrimitiveValue(expr.left.type)
            val rightPrimitive = right.toPrimitiveValue(expr.right.type)

            when (expr.operator) {
                // Sort of pattern matching...
                Add -> when {
                    //  CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-addConcrete
                    //  Add(Integral a, Integral b) ---> Integral (a + b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::plus)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-addA0
                    // Add(a, Integral 0) ---> a
                    right.isIntegralLiteral && right.toLong() == 0L -> left
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-addLiteralToRight
                    // Add(Concrete a, b) ---> Add(b, Concrete a).accept(this)
                    left.isIntegralLiteral -> Add(
                        rightPrimitive,
                        leftPrimitive
                    ).accept(this)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-addAddOpenLiteral
                    // Add(Add(x, Integral a), Integral b)) -> Add(x, Integral (a + b))
                    right.isIntegralLiteral && left is UtOpExpression && left.operator is Add && left.right.expr.isIntegralLiteral ->
                        Add(
                            left.left,
                            evalIntegralLiterals(left.right.expr, right, expr.right.expr.sort, Long::plus)
                                .toPrimitiveValue(expr.right.type)
                        )
                    else -> Add(leftPrimitive, rightPrimitive)
                }
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-subToAdd
                // Sub(a, b) ---> Add(a, Neg b)
                Sub -> Add(
                    leftPrimitive,
                    UtNegExpression(rightPrimitive).toPrimitiveValue(expr.right.type)
                ).accept(this)
                Mul -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulConcrete
                    // Mul(Integral a, Integral b) ---> Integral (a * b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::times)
                    right.isIntegralLiteral -> when {
                        // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulA0
                        // Mul(a, b@(Integral 0)) ---> b
                        right.toLong() == 0L -> right

                        // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulA1
                        // Mul(a, Integral 1) ---> a
                        right.toLong() == 1L -> left

                        // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulAMinus1
                        // Mul(a, Integral -1) ---> Neg a
                        right.toLong() == -1L -> UtNegExpression(leftPrimitive)

                        // Mul(Op(_, Integral _), Integral _)
                        left is UtOpExpression && left.right.expr.isIntegralLiteral -> when (left.operator) {

                            // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulDistr
                            // Mul(Add(x, Integral a), Integral b) ---> Add(Mul(x, b), Integral (a * b))
                            is Add -> Add(
                                Mul(
                                    left.left,
                                    rightPrimitive
                                ).toPrimitiveValue(expr.left.type),
                                evalIntegralLiterals(left.right.expr, right, expr.resultSort, Long::times)
                                    .toPrimitiveValue(expr.right.type)
                            )

                            // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulMulOpenLiteral
                            // Mul(Mul(x, Integral a), Integral b) ---> Mul(x, Integral (a * b))
                            is Mul -> Mul(
                                left.left,
                                evalIntegralLiterals(left.right.expr, right, expr.right.expr.sort, Long::times)
                                    .toPrimitiveValue(expr.right.type)
                            )
                            else -> Mul(leftPrimitive, rightPrimitive)
                        }
                        else -> Mul(leftPrimitive, rightPrimitive)
                    }
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-mulLiteralToRight
                    // Mul(a@(Integral _), b) ---> Mul(b, a)
                    left.isIntegralLiteral -> Mul(
                        rightPrimitive,
                        leftPrimitive
                    ).accept(this)
                    else -> Mul(leftPrimitive, rightPrimitive)
                }
                Div -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-divConcrete
                    // Div(Integral a, Integral b) ---> Integral (a / b)
                    left.isIntegralLiteral && right.isIntegralLiteral && right.toLong() != 0L ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::div)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-divA1
                    // Div(a, Concrete 1) ---> a
                    right.isIntegralLiteral && right.toLong() == 1L -> left
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-divAMinus1
                    // Div(a, Concrete -1) ---> Neg a
                    right.isIntegralLiteral && right.toLong() == -1L -> UtNegExpression(leftPrimitive)
                    else -> Div(leftPrimitive, rightPrimitive)
                }
                And -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andConcrete
                    // And(Integral a, Integral b) ---> Integral (a `and` b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::and)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andA0
                    // And(a@(Integral 0), b) ---> a
                    left.isIntegralLiteral && left.toLong() == 0L ->
                        evalIntegralLiterals(left, expr.resultSort) { it }
                    right.isIntegralLiteral && right.toLong() == 0L ->
                        evalIntegralLiterals(right, expr.resultSort) { it }
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andAtoRight
                    left.isIntegralLiteral -> And(
                        rightPrimitive,
                        leftPrimitive
                    )
                    else -> And(leftPrimitive, rightPrimitive)
                }
                Cmp, Cmpg, Cmpl -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-cmpConcrete
                    // Cmp(Integral a, Integral b) ---> Integral (cmp a b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort) { a, b ->
                            when {
                                a < b -> -1
                                a == b -> 0
                                else -> 1
                            }
                        }
                    else -> expr.operator(leftPrimitive, rightPrimitive)
                }
                Or -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-orConcrete
                    // Or(Integral a, Integral b) ---> Integral (a `or` b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::or)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-orA0
                    // Or(a, b@(Integral 0)) ---> a
                    left.isIntegralLiteral && left.toLong() == 0L -> right
                    right.isIntegralLiteral && right.toLong() == 0L -> left
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-orLiteralToRight
                    // Or(a@(Integral _),b) ---> Or(b, a)
                    left.isIntegralLiteral -> Or(
                        rightPrimitive,
                        leftPrimitive
                    )
                    else -> Or(leftPrimitive, rightPrimitive)
                }
                Shl -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-shlConcrete
                    // Shl(Integral a, Integral b) ---> Integral (a `shl` b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort) { a, b ->
                            val size: Int = (expr.sort as UtBvSort).size
                            a shl (b.toInt() % size)
                        }

                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-shlCycle
                    // Shl(a, Integral b) && b % a.bit_size == 0 ---> a
                    right.isIntegralLiteral && right.toLong() % (expr.resultSort as UtBvSort).size == 0L -> left
                    else -> Shl(leftPrimitive, rightPrimitive)
                }
                Shr -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-shrConcrete
                    // Shr(Integral a, Integral b) ---> Integral (a `shr` b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort) { a, b ->
                            val size: Int = (expr.sort as UtBvSort).size
                            a shr (b.toInt() % size)
                        }
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-shrCycle
                    // Shr(a, Integral b) && b % a.bit_size == 0 ---> a
                    right.isIntegralLiteral && right.toLong() % (expr.resultSort as UtBvSort).size == 0L -> left
                    else -> Shr(leftPrimitive, rightPrimitive)
                }
                Ushr -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-ushrConcrete
                    // Ushr(Integral a, Integral b) ---> Integral (a `ushr` b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort) { a, b ->
                            val size: Int = (expr.sort as UtBvSort).size
                            if (a >= 0) {
                                a ushr (b.toInt() % size)
                            } else {
                                // 0b0..01..1 where mask.countOneBits() = size
                                val mask: Long = ((1L shl size) - 1L)
                                (a and mask) ushr (b.toInt() % size)
                            }
                        }
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-ushrCycle
                    // Ushr(a, Integral b) && b % a.bit_size == 0 ---> a
                    right.isIntegralLiteral && right.toLong() % (expr.resultSort as UtBvSort).size == 0L -> left
                    else -> Ushr(leftPrimitive, rightPrimitive)
                }
                Xor -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-xorConcrete
                    // Xor(Integral a, Integral b) ---> Integral (a `xor` b)
                    left.isIntegralLiteral && right.isIntegralLiteral ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::xor)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-xorA0
                    // Xor(a, Integral 0) ---> a
                    right.isIntegralLiteral && right.toLong() == 0L -> left
                    left.isIntegralLiteral && left.toLong() == 0L -> right
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-xorEqual
                    // Xor(a, a) ---> Integral 0
                    left == right -> (0L).toValueWithSort(expr.resultSort).primitiveToSymbolic().expr
                    else -> Xor(leftPrimitive, rightPrimitive)
                }
                Rem -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-remConcrete
                    // Rem(Integral a, Integral b) ---> Integral (a % b)
                    left.isIntegralLiteral && right.isIntegralLiteral && right.toLong() != 0L ->
                        evalIntegralLiterals(left, right, expr.resultSort, Long::rem)
                    else -> Rem(leftPrimitive, rightPrimitive)
                }
            }
        }


    override fun visit(expr: UtTrue): UtExpression = expr

    override fun visit(expr: UtFalse): UtExpression = expr

    override fun visit(expr: UtEqExpression): UtExpression =
        applySimplification(expr) {
            val left = expr.left.accept(this)
            val right = expr.right.accept(this)
            when {
                // Eq(Integral a, Integral b) ---> a == b
                left.isIntegralLiteral && right.isIntegralLiteral -> evalIntegralLiterals(left, right, Long::equals)
                // Eq(Fp a, Fp b) ---> a == b
                left is UtFpLiteral && right is UtFpLiteral -> mkBool(left.value == right.value)
                // Eq(Expr(sort = Boolean), Integral _)
                left is UtIteExpression -> {
                    val thenEq = UtEqExpression(left.thenExpr, right).accept(this)
                    val elseEq = UtEqExpression(left.elseExpr, right).accept(this)
                    if (thenEq is UtFalse && elseEq is UtFalse) {
                        UtFalse
                    } else if (thenEq is UtTrue && elseEq is UtFalse) {
                        left.condition
                    } else if (thenEq is UtFalse && elseEq is UtTrue) {
                        mkNot(left.condition)
                    } else {
                        UtEqExpression(left, right)
                    }
                }
                right is UtIteExpression -> UtEqExpression(right, left).accept(this)
                left is UtBoolExpression && right.isIntegralLiteral -> when {
                    // Eq(a, true) ---> a
                    right.toLong() == UtLongTrue -> left
                    // Eq(a, false) ---> not a
                    right.toLong() == UtLongFalse -> NotBoolExpression(left).accept(this)
                    else -> UtEqExpression(left, right)
                }
                right.isIntegralLiteral -> simplifyEq(left, right)
                // Eq(a, Fp NaN) ---> False
                right is UtFpLiteral && right.value.toDouble().isNaN() -> UtFalse
                // Eq(a@(Concrete _), b) ---> Eq(b, a)
                left.isConcrete && !right.isConcrete -> UtEqExpression(right, left).accept(this)
                // Eq(a a) && a.sort !is FPSort -> true
                left == right && left.sort !is UtFp32Sort && left.sort !is UtFp64Sort -> UtTrue
                else -> UtEqExpression(left, right)
            }
        }

    override fun visit(expr: UtBoolConst): UtExpression = applySimplification(expr, false)

    private fun negate(expr: UtBoolLiteral): UtBoolLiteral = mkBool(expr == UtFalse)

    override fun visit(expr: NotBoolExpression): UtExpression =
        applySimplification(expr) {
            when (val exp = expr.expr.accept(this) as UtBoolExpression) {
                // not true ---> false
                // not false ---> true
                is UtBoolLiteral -> negate(exp)
                is UtBoolOpExpression -> {
                    val left = exp.left
                    val right = exp.right
                    if (left.type is IntegerType && right.type is IntegerType) {
                        when (exp.operator) {
                            // not Ne(a, b) ---> Eq(a, b)
                            Ne -> Eq(left, right)
                            // not Ge(a, b) ---> Lt(a, b)
                            Ge -> Lt(left, right)
                            // not Gt(a, b) ---> Le(a, b)
                            Gt -> Le(left, right)
                            // not Lt(a, b) ---> Ge(a, b)
                            Lt -> Ge(left, right)
                            // not Le(a, b) ---> Gt(a, b)
                            Le -> Gt(left, right)
                            Eq -> NotBoolExpression(exp)
                        }
                    } else {
                        when (exp.operator) {
                            // simplification not Ne(a, b) ---> Eq(a, b) is always valid
                            Ne -> Eq(left, right)
                            else -> NotBoolExpression(exp)
                        }
                    }
                }
                // not not expr -> expr
                is NotBoolExpression -> exp.expr
                // not (and a_1, a_2, ..., a_n) ---> or (not a_1), (not a_2), ..., (not a_n)
                is UtAndBoolExpression -> UtOrBoolExpression(exp.exprs.map { NotBoolExpression(it).accept(this) as UtBoolExpression })
                // not (or a_1, a_2, ..., a_n) ---> and (not a_1), (not a_2), ..., (not a_n)
                is UtOrBoolExpression -> UtAndBoolExpression(exp.exprs.map { NotBoolExpression(it).accept(this) as UtBoolExpression })
                else -> NotBoolExpression(exp)
            }
        }

    override fun visit(expr: UtOrBoolExpression): UtExpression =
        applySimplification(expr) {
            val exprs = expr.exprs.map { it.accept(this) as UtBoolExpression }.filterNot { it == UtFalse }
                .flatMap { splitOr(it) }
            when {
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andOrEmpty
                exprs.isEmpty() -> UtFalse
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andOrValue
                exprs.any { it == UtTrue } -> UtTrue
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andorSingle
                exprs.size == 1 -> exprs.single()
                else -> UtOrBoolExpression(exprs)
            }
        }

    override fun visit(expr: UtAndBoolExpression): UtExpression =
        applySimplification(expr) {
            val exprs = expr.exprs.map { it.accept(this) as UtBoolExpression }.filterNot { it == UtTrue }
                .flatMap { splitAnd(it) }
            when {
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andOrEmpty
                exprs.isEmpty() -> UtTrue
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andOrValue
                exprs.any { it == UtFalse } -> UtFalse
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-andOrSingle
                exprs.size == 1 -> exprs.single()
                else -> UtAndBoolExpression(exprs)
            }
        }

    override fun visit(expr: UtAddNoOverflowExpression): UtExpression =
        UtAddNoOverflowExpression(
            applySimplification(expr.left),
            applySimplification(expr.right)
        )

    override fun visit(expr: UtSubNoOverflowExpression): UtExpression =
        UtSubNoOverflowExpression(
            applySimplification(expr.left),
            applySimplification(expr.right)
        )

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-negConcrete
    // Neg (Concrete a) ---> Concrete (-a)
    override fun visit(expr: UtNegExpression): UtExpression =
        applySimplification(expr) {
            val variable = expr.variable.expr.accept(this)
            if (variable.isConcrete) {
                val value = variable.toConcrete()
                when (variable.sort) {
                    UtByteSort -> mkInt(-(value as Byte))
                    UtShortSort -> mkInt(-(value as Short))
                    UtIntSort -> mkInt(-(value as Int))
                    UtLongSort -> mkLong(-(value as Long))
                    UtFp32Sort -> mkFloat(-(value as Float))
                    UtFp64Sort -> mkDouble(-(value as Double))
                    else -> UtNegExpression(variable.toPrimitiveValue(expr.variable.type))
                }
            } else {
                UtNegExpression(variable.toPrimitiveValue(expr.variable.type))
            }
        }

    override fun visit(expr: UtCastExpression): UtExpression =
        applySimplification(expr) {
            val newExpr = expr.variable.expr.accept(this)
            when {
                // Cast(a@(PrimitiveValue type), type) ---> a
                expr.type == expr.variable.type -> newExpr
                newExpr.isConcrete -> when (newExpr) {
                    is UtBoolLiteral -> newExpr.toLong().toValueWithSort(expr.sort).primitiveToSymbolic().expr
                    is UtBvLiteral -> newExpr.toLong().toValueWithSort(expr.sort).primitiveToSymbolic().expr
                    is UtFpLiteral -> newExpr.value.toValueWithSort(expr.sort).primitiveToSymbolic().expr
                    else -> UtCastExpression(newExpr.toPrimitiveValue(expr.variable.type), expr.type)
                }
                else -> UtCastExpression(newExpr.toPrimitiveValue(expr.variable.type), expr.type)
            }
        }


    /**
     * Simplify expression [Eq](left, right), where [right] operand is integral literal,
     * using information about lower and upper bounds of [left] operand from [lts] and [gts] maps.
     *
     * @return
     * [UtFalse], if lower bound of [left] is greater than [right].value
     *            or upper bound of [left] is less than [right].value
     *
     * [Eq](left, right), otherwise
     */
    private fun simplifyEq(left: PrimitiveValue, right: PrimitiveValue): UtBoolExpression {
        val leftExpr = if (left.expr is UtAddrExpression) left.expr.internal else left.expr
        val lowerBound = gts[leftExpr]
        val upperBound = lts[leftExpr]

        return if (lowerBound == null && upperBound == null) {
            Eq(left, right)
        } else {
            val rightValue = right.expr.toLong()
            when {
                lowerBound != null && lowerBound > rightValue -> UtFalse
                upperBound != null && upperBound < rightValue -> UtFalse
                else -> Eq(left, right)
            }
        }
    }

    private fun simplifyEq(left: UtExpression, right: UtExpression): UtBoolExpression {
        val leftExpr = if (left is UtAddrExpression) left.internal else left
        val lowerBound = gts[leftExpr]
        val upperBound = lts[leftExpr]
        return if (lowerBound == null && upperBound == null) {
            UtEqExpression(left, right)
        } else {
            val rightValue = right.toLong()
            when {
                lowerBound != null && lowerBound > rightValue -> UtFalse
                upperBound != null && upperBound < rightValue -> UtFalse
                else -> UtEqExpression(left, right)
            }
        }
    }


    /**
     * Simplify expression [Gt](left, right) or [Ge](left, right), where [right] operand is integral literal,
     * using information about lower and upper bounds of [left] operand from [lts] and [gts] maps.
     *
     * @param including - if true than simplified expression is Ge(left,right), else Gt(left, right)
     * @return
     * [UtTrue], if lower bound of [left] is greater than [right].value.
     *
     * [UtFalse], if upper bound of [left] is less than [right].value.
     *
     * [Eq](left, right), if interval [[right].value, upper bound of [left]] has length = 1.
     *
     * [Gt]|[Ge](left, right), otherwise, depending on value of [including] parameter.
     */
    private fun simplifyGreater(left: PrimitiveValue, right: PrimitiveValue, including: Boolean): UtExpression {
        val leftExpr = if (left.expr is UtAddrExpression) left.expr.internal else left.expr
        val lowerBound = gts[leftExpr]
        val upperBound = lts[leftExpr]
        val operator = if (including) Ge else Gt

        return if (lowerBound == null && upperBound == null) {
            operator(left, right)
        } else {
            val rightValue = right.expr.toLong() + if (including) 0 else 1
            when {
                // left > Long.MAX_VALUE
                rightValue == Long.MIN_VALUE && !including -> UtFalse
                lowerBound != null && lowerBound >= rightValue -> UtTrue
                upperBound != null && upperBound < rightValue -> UtFalse
                upperBound != null && rightValue - upperBound == 0L -> Eq(
                    left,
                    rightValue.toValueWithSort(right.expr.sort).primitiveToSymbolic()
                )
                else -> operator(left, right)
            }
        }
    }

    /**
     * Simplify expression [Lt](left, right) or [Le](left, right), where [right] operand is integral literal,
     * using information about lower and upper bounds of [left] operand from [lts] and [gts] maps.
     *
     * @param including - if true than simplified expression is Le(left, right), else Lt(left, right)
     * @return
     * [UtTrue], if upper bound of [left] is less than [right].value.
     *
     * [UtFalse], if lower bound of [left] is greater than [right].value.
     *
     * [Eq](left, right), if interval [lower bound of [left], [right].value] has length = 1.
     *
     * [Lt]|[Le](left, right), otherwise, depending on value of [including] parameter.
     */
    private fun simplifyLess(left: PrimitiveValue, right: PrimitiveValue, including: Boolean): UtExpression {
        val leftExpr = if (left.expr is UtAddrExpression) left.expr.internal else left.expr
        val lowerBound = gts[leftExpr]
        val upperBound = lts[leftExpr]
        val operator = if (including) Le else Lt

        return if (upperBound == null && lowerBound == null) {
            operator(left, right)
        } else {
            val rightValue = right.expr.toLong() - if (including) 0 else 1
            when {
                // left < Long.MIN_VALUE
                rightValue == Long.MAX_VALUE && !including -> UtFalse
                upperBound != null && upperBound <= rightValue -> UtTrue
                lowerBound != null && lowerBound > rightValue -> UtFalse
                lowerBound != null && rightValue - lowerBound == 0L -> Eq(
                    left,
                    rightValue.toValueWithSort(right.expr.sort).primitiveToSymbolic()
                )
                else -> operator(left, right)
            }
        }
    }


    override fun visit(expr: UtBoolOpExpression): UtExpression =
        applySimplification(expr) {
            val left = expr.left.expr.accept(this)
            val right = expr.right.expr.accept(this)
            val leftPrimitive = left.toPrimitiveValue(expr.left.type)
            val rightPrimitive = right.toPrimitiveValue(expr.right.type)
            when (expr.operator) {
                Eq -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqConcrete
                    // Eq(Integral a, Integral b) ---> a == b
                    left.isIntegralLiteral && right.isIntegralLiteral -> evalIntegralLiterals(left, right, Long::equals)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqTrue
                    // Eq(a, true) ---> a
                    left is UtBoolExpression && right.isIntegralLiteral && right.toLong() == UtLongTrue -> left
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqFalse
                    // Eq(a, false) ---> not a
                    left is UtBoolExpression && right.isIntegralLiteral && right.toLong() == UtLongFalse ->
                        NotBoolExpression(left).accept(this)
                    // Eq(Op(_, Integral _), Integral _)
                    right.isIntegralLiteral && left is UtOpExpression && left.right.expr.isIntegralLiteral -> when (left.operator) {
                        // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqAddOpenLiteral
                        // Eq(Add(a, Integral b), Integral c) ---> Eq(a, Integral (c - b))
                        Add -> Eq(
                            left.left,
                            evalIntegralLiterals(
                                right,
                                left.right.expr,
                                maxSort(expr.right, left.right),
                                Long::minus
                            ).toPrimitiveValue(maxType(expr.right.type, left.right.type))
                        )
                        // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqXorOpenLiteral
                        // Eq(Xor(a, Integral b), Integral c) ---> Eq(a, Integral (c `xor` b))
                        Xor -> Eq(
                            left.left,
                            evalIntegralLiterals(right, left.right.expr, maxSort(expr.right, left.right), Long::xor)
                                .toPrimitiveValue(maxType(expr.right.type, left.right.type))
                        )
                        else -> Eq(leftPrimitive, rightPrimitive)
                    }
                    /**
                     * @see simplifyEq
                     */
                    right.isIntegralLiteral -> simplifyEq(leftPrimitive, rightPrimitive)
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqConcrete
                    // Eq(Fp a, Fp b) ---> a == b
                    left is UtFpLiteral && right is UtFpLiteral -> mkBool(left.value == right.value)

                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqNaN
                    // Eq(a, Fp NaN) ---> False
                    left is UtFpLiteral && left.value.toDouble().isNaN() -> UtFalse
                    right is UtFpLiteral && right.value.toDouble().isNaN() -> UtFalse
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqEqual
                    // Eq(a a) && a.sort !is FPSort -> true
                    left == right && expr.left.type !is FloatType && expr.left.type !is DoubleType -> UtTrue
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqLiteralToRight
                    // Eq(a@(Concrete _), b) ---> Eq(b, a)
                    left.isConcrete -> Eq(
                        rightPrimitive,
                        leftPrimitive
                    ).accept(this)
                    else -> Eq(leftPrimitive, rightPrimitive)
                }
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-neToNotEq
                // Ne -> not Eq
                Ne -> NotBoolExpression(
                    Eq(
                        leftPrimitive,
                        rightPrimitive
                    )
                ).accept(this)
                Le -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-leConcrete
                    // Le(Integral a, Integral b) ---> a <= b
                    left.isIntegralLiteral && right.isIntegralLiteral -> {
                        evalIntegralLiterals(left, right) { a, b -> a <= b }
                    }
                    /**
                     * @see simplifyLess
                     * @see simplifyGreater
                     */
                    right.isIntegralLiteral -> simplifyLess(leftPrimitive, rightPrimitive, true)
                    left.isIntegralLiteral -> simplifyGreater(rightPrimitive, leftPrimitive, true)
                    else -> Le(leftPrimitive, rightPrimitive)
                }
                Lt -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-ltConcrete
                    // Lt(Integral a, Integral b) ---> a < b
                    left.isIntegralLiteral && right.isIntegralLiteral -> evalIntegralLiterals(left, right) { a, b ->
                        a < b
                    }
                    /**
                     * @see simplifyLess
                     * @see simplifyGreater
                     */
                    right.isIntegralLiteral -> simplifyLess(leftPrimitive, rightPrimitive, false)
                    left.isIntegralLiteral -> simplifyGreater(rightPrimitive, leftPrimitive, false)
                    else -> Lt(leftPrimitive, rightPrimitive)
                }
                Ge -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-geConcrete
                    // Ge(Integral a, Integral b) ---> a >= b
                    left.isIntegralLiteral && right.isIntegralLiteral -> evalIntegralLiterals(left, right) { a, b ->
                        a >= b
                    }
                    /**
                     * @see simplifyLess
                     * @see simplifyGreater
                     */
                    right.isIntegralLiteral -> simplifyGreater(leftPrimitive, rightPrimitive, true)
                    left.isIntegralLiteral -> simplifyLess(rightPrimitive, leftPrimitive, true)
                    else -> Ge(leftPrimitive, rightPrimitive)
                }
                Gt -> when {
                    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-gtConcrete
                    // Gt(Integral a, Integral b) ---> a > b
                    left.isIntegralLiteral && right.isIntegralLiteral -> evalIntegralLiterals(left, right) { a, b ->
                        a > b
                    }
                    /**
                     * @see simplifyLess
                     * @see simplifyGreater
                     */
                    right.isIntegralLiteral -> simplifyGreater(leftPrimitive, rightPrimitive, false)
                    left.isIntegralLiteral -> simplifyLess(rightPrimitive, leftPrimitive, false)
                    else -> Gt(leftPrimitive, rightPrimitive)
                }
            }
        }

    private fun maxType(left: Type, right: Type): Type = when {
        left is LongType -> left
        right is LongType -> right
        else -> IntType.v()
    }

    override fun visit(expr: UtIsExpression): UtExpression = applySimplification(expr, false) {
        UtIsExpression(expr.addr.accept(this) as UtAddrExpression, expr.typeStorage, expr.numberOfTypes)
    }

    override fun visit(expr: UtGenericExpression): UtExpression = applySimplification(expr, false) {
        UtGenericExpression(expr.addr.accept(this) as UtAddrExpression, expr.types, expr.numberOfTypes)
    }

    override fun visit(expr: UtIsGenericTypeExpression): UtExpression = applySimplification(expr, false) {
        UtIsGenericTypeExpression(
            expr.addr.accept(this) as UtAddrExpression,
            expr.baseAddr.accept(this) as UtAddrExpression,
            expr.parameterTypeIndex
        )
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): UtExpression =
        applySimplification(expr, false) {
            UtEqGenericTypeParametersExpression(
                expr.firstAddr.accept(this) as UtAddrExpression,
                expr.secondAddr.accept(this) as UtAddrExpression,
                expr.indexMapping
            )
        }

    override fun visit(expr: UtInstanceOfExpression): UtExpression = applySimplification(expr, false) {
        val simplifiedHard = (expr.constraint.accept(this) as UtBoolExpression).asHardConstraint()
        UtInstanceOfExpression(expr.symbolicStateUpdate.copy(hardConstraints = simplifiedHard))
    }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-ite
    // ite(true, then, _)  ---> then
    // ite(false, _, else) ---> else
    override fun visit(expr: UtIteExpression): UtExpression =
        applySimplification(expr) {
            when (val condition = expr.condition.accept(this) as UtBoolExpression) {
                UtTrue -> expr.thenExpr.accept(this)
                UtFalse -> expr.elseExpr.accept(this)
                else -> UtIteExpression(condition, expr.thenExpr.accept(this), expr.elseExpr.accept(this))
            }
        }

    override fun visit(expr: UtMkTermArrayExpression): UtExpression = applySimplification(expr, false)

    override fun visit(expr: UtStringConst): UtExpression = expr

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sConcat
    // UtConcat("A_1", "A_2", ..., "A_n") ---> "A_1A_2...A_n"
    override fun visit(expr: UtConcatExpression): UtExpression =
        applySimplification(expr) {
            val parts = expr.parts.map { it.accept(this) as UtStringExpression }
            if (parts.all { it.isConcrete }) {
                UtSeqLiteral(parts.joinToString { it.toConcrete() as String })
            } else {
                UtConcatExpression(parts)
            }
        }

    override fun visit(expr: UtConvertToString): UtExpression =
        applySimplification(expr) { UtConvertToString(expr.expression.accept(this)) }

    override fun visit(expr: UtStringToInt): UtExpression =
        applySimplification(expr) { UtStringToInt(expr.expression.accept(this), expr.sort) }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sLength
    // UtLength "A" ---> "A".length
    override fun visit(expr: UtStringLength): UtExpression =
        applySimplification(expr) {
            val string = expr.string.accept(this)
            when {
                string is UtArrayToString -> string.length.expr
                string.isConcrete -> mkInt((string.toConcrete() as String).length)
                else -> UtStringLength(string)
            }
        }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sPositiveLength
    // UtPositiveLength "A" ---> UtTrue
    override fun visit(expr: UtStringPositiveLength): UtExpression =
        applySimplification(expr) {
            val string = expr.string.accept(this)
            if (string.isConcrete) UtTrue else UtStringPositiveLength(string)
        }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sCharAt
    // UtCharAt "A" (Integral i) ---> "A".charAt(i)
    override fun visit(expr: UtStringCharAt): UtExpression =
        applySimplification(expr) {
            val index = expr.index.accept(this)
            val string = withNewSelect(index) { expr.string.accept(this) }
            if (allConcrete(string, index)) {
                (string.toConcrete() as String)[index.toConcrete() as Int].primitiveToSymbolic().expr
            } else {
                UtStringCharAt(string, index)
            }
        }

    override fun visit(expr: UtStringEq): UtExpression =
        applySimplification(expr) {
            val left = expr.left.accept(this)
            val right = expr.right.accept(this)
            when {
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqConcrete
                // Eq("A", "B") ---> "A" == "B"
                allConcrete(left, right) -> mkBool(left.toConcrete() == right.toConcrete())
                // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-eqEqual
                // Eq(a, a) ---> true
                left == right -> UtTrue
                else -> UtStringEq(left, right)
            }
        }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sSubstring
    // UtSubstring "A" (Integral begin) (Integral end) ---> "A".substring(begin, end)
    override fun visit(expr: UtSubstringExpression): UtExpression =
        applySimplification(expr) {
            val string = expr.string.accept(this)
            val beginIndex = expr.beginIndex.accept(this)
            val length = expr.length.accept(this)
            if (allConcrete(string, beginIndex, length)) {
                val begin = beginIndex.toConcrete() as Int
                val end = begin + length.toConcrete() as Int
                UtSeqLiteral((string.toConcrete() as String).substring(begin, end))
            } else {
                UtSubstringExpression(string, beginIndex, length)
            }
        }

    override fun visit(expr: UtReplaceExpression): UtExpression = applySimplification(expr, false)

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sStartsWith
    // UtStartsWith "A" "P" ---> "A".startsWith("P")
    override fun visit(expr: UtStartsWithExpression): UtExpression =
        applySimplification(expr) {
            val string = expr.string.accept(this)
            val prefix = expr.prefix.accept(this)
            if (allConcrete(string, prefix)) {
                val concreteString = string.toConcrete() as String
                val concretePrefix = prefix.toConcrete() as String
                mkBool(concreteString.startsWith(concretePrefix))
            } else {
                UtStartsWithExpression(string, prefix)
            }
        }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sEndsWith
    // UtEndsWith "A" "S" ---> "A".endsWith("S")
    override fun visit(expr: UtEndsWithExpression): UtExpression =
        applySimplification(expr) {
            val string = expr.string.accept(this)
            val suffix = expr.suffix.accept(this)
            if (allConcrete(string, suffix)) {
                val concreteString = string.toConcrete() as String
                val concreteSuffix = suffix.toConcrete() as String
                mkBool(concreteString.endsWith(concreteSuffix))
            } else {
                UtEndsWithExpression(string, suffix)
            }
        }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sIndexOf
    // UtIndexOf "A" "S" ---> "A".indexOf("S")
    override fun visit(expr: UtIndexOfExpression): UtExpression = applySimplification(expr) {
        val string = expr.string.accept(this)
        val substring = expr.substring.accept(this)
        if (allConcrete(string, substring)) {
            val concreteString = string.toConcrete() as String
            val concreteSubstring = substring.toConcrete() as String
            mkInt(concreteString.indexOf(concreteSubstring))
        } else {
            UtIndexOfExpression(string, substring)
        }
    }

    // CONFLUENCE:UtBot+Expression+Optimizations#UtBotExpressionOptimizations-sContains
    // UtContains "A" "S" ---> "A".contains("S")
    override fun visit(expr: UtContainsExpression): UtExpression =
        applySimplification(expr) {
            val string = expr.string.accept(this)
            val substring = expr.substring.accept(this)
            if (allConcrete(string, substring)) {
                val concreteString = string.toConcrete() as String
                val concreteSubstring = substring.toConcrete() as String
                mkBool(concreteString.contains(concreteSubstring))
            } else {
                UtContainsExpression(string, substring)
            }
        }

    override fun visit(expr: UtToStringExpression): UtExpression =
        applySimplification(expr) {
            UtToStringExpression(expr.isNull.accept(this) as UtBoolExpression, expr.notNullExpr.accept(this))
        }

    override fun visit(expr: UtArrayToString): UtExpression =
        applySimplification(expr) {
            UtArrayToString(
                expr.arrayExpression.accept(this),
                expr.offset.expr.accept(this).toIntValue(),
                expr.length.expr.accept(this).toIntValue()
            )
        }

    override fun visit(expr: UtSeqLiteral): UtExpression = expr

    override fun visit(expr: UtConstArrayExpression): UtExpression =
        applySimplification(expr) {
            UtConstArrayExpression(expr.constValue.accept(this), expr.sort)
        }

    override fun visit(expr: UtArrayInsert): UtExpression = applySimplification(expr, false) {
        UtArrayInsert(
            expr.arrayExpression.accept(this),
            expr.index.expr.accept(this).toIntValue(),
            expr.element.accept(this)
        )
    }

    override fun visit(expr: UtArrayInsertRange): UtExpression = applySimplification(expr, false) {
        UtArrayInsertRange(
            expr.arrayExpression.accept(this),
            expr.index.expr.accept(this).toIntValue(),
            expr.elements.accept(this),
            expr.from.expr.accept(this).toIntValue(),
            expr.length.expr.accept(this).toIntValue()
        )
    }

    override fun visit(expr: UtArrayRemove): UtExpression = applySimplification(expr, false) {
        UtArrayRemove(
            expr.arrayExpression.accept(this),
            expr.index.expr.accept(this).toIntValue()
        )
    }


    override fun visit(expr: UtArrayRemoveRange): UtExpression = applySimplification(expr, false) {
        UtArrayRemoveRange(
            expr.arrayExpression.accept(this),
            expr.index.expr.accept(this).toIntValue(),
            expr.length.expr.accept(this).toIntValue()
        )

    }

    override fun visit(expr: UtArraySetRange): UtExpression = applySimplification(expr, false) {
        UtArraySetRange(
            expr.arrayExpression.accept(this),
            expr.index.expr.accept(this).toIntValue(),
            expr.elements.accept(this),
            expr.from.expr.accept(this).toIntValue(),
            expr.length.expr.accept(this).toIntValue()
        )
    }

    override fun visit(expr: UtArrayShiftIndexes): UtExpression = applySimplification(expr, false) {
        UtArrayShiftIndexes(
            expr.arrayExpression.accept(this),
            expr.offset.expr.accept(this).toIntValue()
        )
    }

    override fun visit(expr: UtArrayApplyForAll): UtExpression = applySimplification(expr, false) {
        UtArrayApplyForAll(
            expr.arrayExpression.accept(this),
            expr.constraint
        )
    }

    override fun visit(expr: UtStringToArray): UtExpression = applySimplification(expr, false) {
        UtStringToArray(
            expr.stringExpression.accept(this),
            expr.offset.expr.accept(this).toIntValue()
        )
    }
}


private val arrayExpressionAxiomInstantiationCache =
    IdentityHashMap<UtExtendedArrayExpression, UtMkArrayExpression>()

private val stringExpressionAxiomInstantiationCache =
    IdentityHashMap<UtStringExpression, UtStringConst>()

private val arrayExpressionAxiomIndex = AtomicInteger(0)

private fun instantiateArrayAsNewConst(arrayExpression: UtExtendedArrayExpression) =
    arrayExpressionAxiomInstantiationCache.getOrPut(arrayExpression) {
        val suffix = when (arrayExpression) {
            is UtArrayInsert -> "Insert"
            is UtArrayInsertRange -> "InsertRange"
            is UtArrayRemove -> "Remove"
            is UtArrayRemoveRange -> "RemoveRange"
            is UtArraySetRange -> "SetRange"
            is UtArrayShiftIndexes -> "ShiftIndexes"
            is UtStringToArray -> "StringToArray"
            is UtArrayApplyForAll -> error("UtArrayApplyForAll cannot be instantiated as new const array")
        }
        UtMkArrayExpression(
            "_array$suffix${arrayExpressionAxiomIndex.getAndIncrement()}",
            arrayExpression.sort
        )
    }

private fun instantiateStringAsNewConst(stringExpression: UtStringExpression) =
    stringExpressionAxiomInstantiationCache.getOrPut(stringExpression) {
        val suffix = when (stringExpression) {
            is UtArrayToString -> "ArrayToString"
            else -> unreachableBranch("Cannot instantiate new string const for $stringExpression")
        }
        UtStringConst("_str$suffix${arrayExpressionAxiomIndex.getAndIncrement()}")
    }

/**
 * Visitor that applies the same simplifications as [RewritingVisitor] and instantiate axioms for extended array theory.
 *
 * @see UtExtendedArrayExpression
 */
class AxiomInstantiationRewritingVisitor(
    eqs: Map<UtExpression, UtExpression> = emptyMap(),
    lts: Map<UtExpression, Long> = emptyMap(),
    gts: Map<UtExpression, Long> = emptyMap()
) : RewritingVisitor(eqs, lts, gts) {
    private val instantiatedAxiomExpressions = mutableListOf<UtBoolExpression>()

    /**
     * Select(UtArrayInsert(a, v, i), j) is equivalent to ITE(i = j, v, Select(a, ITE(j < i, j, j - 1))
     */
    override fun visit(expr: UtArrayInsert): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val index = expr.index.expr.accept(this).toPrimitiveValue(expr.index.type)
        val element = expr.element.accept(this)
        val selectedIndex = selectIndexStack.last()
        val pushedIndex = UtIteExpression(
            Lt(selectedIndex, index),
            selectedIndex.expr,
            Add(selectedIndex, (-1).toPrimitiveValue())
        )
        val arrayExpression = withNewSelect(pushedIndex) { expr.arrayExpression.accept(this) }

        instantiatedAxiomExpressions += UtEqExpression(
            UtIteExpression(
                Eq(selectedIndex, index),
                element,
                arrayExpression.select(pushedIndex),
            ).accept(this), arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    /**
     * Select(UtArrayInsertRange(a, i, b, from, length), j) is equivalent to
     * ITE(j >= i && j < i + length, Select(b, j - i + from), Select(a, ITE(j < i, j, j - length)
     */
    override fun visit(expr: UtArrayInsertRange): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val index = expr.index.expr.accept(this).toPrimitiveValue(expr.index.type)
        val from = expr.from.expr.accept(this).toPrimitiveValue(expr.index.type)
        val length = expr.length.expr.accept(this).toPrimitiveValue(expr.length.type)
        val selectedIndex = selectIndexStack.last()
        val pushedArrayInstanceIndex =
            UtIteExpression(Lt(selectedIndex, index), selectedIndex.expr, Sub(selectedIndex, length))
        val arrayExpression = withNewSelect(pushedArrayInstanceIndex) { expr.arrayExpression.accept(this) }
        val pushedElementsIndex = Add(Sub(selectedIndex, index).toIntValue(), from)
        val elements = withNewSelect(pushedElementsIndex) { expr.elements.accept(this) }
        instantiatedAxiomExpressions += UtEqExpression(
            UtIteExpression(
                mkAnd(Ge(selectedIndex, index), Lt(selectedIndex, Add(index, length).toIntValue())),
                elements.select(pushedElementsIndex),
                arrayExpression.select(pushedArrayInstanceIndex),
            ).accept(this), arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    /**
     * Select(UtArrayRemove(a, i), j) is equivalent to Select(a, ITE(j < i, j, j + 1))
     */
    override fun visit(expr: UtArrayRemove): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val index = expr.index.expr.accept(this).toPrimitiveValue(expr.index.type)
        val selectedIndex = selectIndexStack.last()
        val pushedIndex = UtIteExpression(
            Lt(selectedIndex, index),
            selectedIndex.expr,
            Add(selectedIndex, 1.toPrimitiveValue())
        )
        val arrayExpression = withNewSelect(pushedIndex) { expr.arrayExpression.accept(this) }

        instantiatedAxiomExpressions += UtEqExpression(
            arrayExpression.select(pushedIndex),
            arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    /**
     * Select(UtArrayRemoveRange(a, i, length), j) is equivalent to Select(a, ITE(j < i, j, j + length))
     */
    override fun visit(expr: UtArrayRemoveRange): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val index = expr.index.expr.accept(this).toPrimitiveValue(expr.index.type)
        val length = expr.length.expr.accept(this).toPrimitiveValue(expr.length.type)
        val selectedIndex = selectIndexStack.last()
        val pushedIndex = UtIteExpression(
            Lt(selectedIndex, index),
            selectedIndex.expr,
            Add(selectedIndex, length)
        )
        val arrayExpression = withNewSelect(pushedIndex) { expr.arrayExpression.accept(this) }

        instantiatedAxiomExpressions += UtEqExpression(
            arrayExpression.select(pushedIndex),
            arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    /**
     * Select(UtSetRange(a, i, b, from, length), j) is equivalent to
     * ITE(j >= i && j < i + length, Select(b, j - i + from), Select(a, i))
     */
    override fun visit(expr: UtArraySetRange): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val index = expr.index.expr.accept(this).toPrimitiveValue(expr.index.type)
        val from = expr.from.expr.accept(this).toPrimitiveValue(expr.index.type)
        val length = expr.length.expr.accept(this).toPrimitiveValue(expr.length.type)
        val selectedIndex = selectIndexStack.last()
        val arrayExpression = expr.arrayExpression.accept(this)
        val pushedIndex = Add(Sub(selectedIndex, index).toIntValue(), from)
        val elements = withNewSelect(pushedIndex) { expr.elements.accept(this) }
        instantiatedAxiomExpressions += UtEqExpression(
            UtIteExpression(
                mkAnd(Ge(selectedIndex, index), Lt(selectedIndex, Add(index, length).toIntValue())),
                elements.select(pushedIndex),
                arrayExpression.select(selectedIndex.expr)
            ).accept(this), arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    /**
     * Select(UtShiftIndexes(a, offset), j) is equivalent to Select(a, j - offset)
     */
    override fun visit(expr: UtArrayShiftIndexes): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val offset = expr.offset.expr.accept(this).toIntValue()
        val selectedIndex = selectIndexStack.last()
        val pushedIndex = Sub(selectedIndex, offset)
        val arrayExpression = withNewSelect(pushedIndex) { expr.arrayExpression.accept(this) }

        instantiatedAxiomExpressions += UtEqExpression(
            arrayExpression.select(pushedIndex),
            arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    /**
     * instantiate expr.constraint on selecting from UtArrayApplyForAll
     */
    override fun visit(expr: UtArrayApplyForAll): UtExpression {
        val selectedIndex = selectIndexStack.last()
        val arrayExpression = expr.arrayExpression.accept(this)
        val constraint = expr.constraint(arrayExpression, selectedIndex)
        instantiatedAxiomExpressions += constraint
        return arrayExpression
    }

    override fun visit(expr: UtStringToArray): UtExpression {
        val arrayInstance = instantiateArrayAsNewConst(expr)
        val offset = expr.offset.expr.accept(this).toIntValue()
        val selectedIndex = selectIndexStack.last()
        val pushedIndex = Add(selectedIndex, offset)
        val stringExpression = withNewSelect(pushedIndex) { expr.stringExpression.accept(this) }

        instantiatedAxiomExpressions += UtEqExpression(
            UtStringCharAt(stringExpression, pushedIndex),
            arrayInstance.select(selectedIndex.expr)
        )
        return arrayInstance
    }

    override fun visit(expr: UtArrayToString): UtExpression {
        val stringInstance = instantiateStringAsNewConst(expr)
        val offset = expr.offset.expr.accept(this).toIntValue()
        val selectedIndex = selectIndexStack.last()
        val pushedIndex = Add(selectedIndex, offset)
        val arrayExpression = withNewSelect(pushedIndex) { expr.arrayExpression.accept(this) }

        instantiatedAxiomExpressions += UtEqExpression(
            arrayExpression.select(pushedIndex),
            UtStringCharAt(stringInstance, selectedIndex.expr)
        )
        return stringInstance
    }

    val instantiatedArrayAxioms: List<UtBoolExpression>
        get() = instantiatedAxiomExpressions
}

private const val UtLongTrue = 1L
private const val UtLongFalse = 0L
