package org.utbot.engine

import org.utbot.engine.pc.UtBoolOpExpression
import org.utbot.engine.pc.UtBoolSort
import org.utbot.engine.pc.UtBvSort
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtFp32Sort
import org.utbot.engine.pc.UtFp64Sort
import org.utbot.engine.pc.UtIntSort
import org.utbot.engine.pc.UtOpExpression
import org.utbot.engine.pc.UtSort
import org.utbot.engine.pc.alignSort
import org.utbot.engine.z3.BinOperator
import org.utbot.engine.z3.BoolOperator
import kotlin.reflect.KClass
import soot.jimple.BinopExpr
import soot.jimple.internal.JAddExpr
import soot.jimple.internal.JAndExpr
import soot.jimple.internal.JCmpExpr
import soot.jimple.internal.JCmpgExpr
import soot.jimple.internal.JCmplExpr
import soot.jimple.internal.JDivExpr
import soot.jimple.internal.JEqExpr
import soot.jimple.internal.JGeExpr
import soot.jimple.internal.JGtExpr
import soot.jimple.internal.JLeExpr
import soot.jimple.internal.JLtExpr
import soot.jimple.internal.JMulExpr
import soot.jimple.internal.JNeExpr
import soot.jimple.internal.JOrExpr
import soot.jimple.internal.JRemExpr
import soot.jimple.internal.JShlExpr
import soot.jimple.internal.JShrExpr
import soot.jimple.internal.JSubExpr
import soot.jimple.internal.JUshrExpr
import soot.jimple.internal.JXorExpr

private val ops: Map<KClass<*>, UtOperator<*>> = mapOf(
    JLeExpr::class to Le,
    JLtExpr::class to Lt,
    JGeExpr::class to Ge,
    JGtExpr::class to Gt,
    JEqExpr::class to Eq,
    JNeExpr::class to Ne,
    JDivExpr::class to Div,
    JRemExpr::class to Rem,
    JMulExpr::class to Mul,
    JAddExpr::class to Add,
    JSubExpr::class to Sub,
    JShlExpr::class to Shl,
    JShrExpr::class to Shr,
    JUshrExpr::class to Ushr,
    JXorExpr::class to Xor,
    JOrExpr::class to Or,
    JAndExpr::class to And,
    JCmpExpr::class to Cmp,
    JCmplExpr::class to Cmpl,
    JCmpgExpr::class to Cmpg
)

fun doOperation(
    sootExpression: BinopExpr,
    left: PrimitiveValue,
    right: PrimitiveValue
): UtExpression =
    ops[sootExpression::class]!!(left, right)

sealed class UtOperator<T : UtExpression>(val delegate: BinOperator) {
    abstract operator fun invoke(left: PrimitiveValue, right: PrimitiveValue): T

    override fun toString(): String = this.javaClass.simpleName
}

sealed class UtBinOperator(
    delegate: BinOperator,
    private val sort: (PrimitiveValue, PrimitiveValue) -> UtSort = ::maxSort
) : UtOperator<UtOpExpression>(delegate) {
    override operator fun invoke(left: PrimitiveValue, right: PrimitiveValue): UtOpExpression =
        UtOpExpression(this, left, right, sort(left, right))
}

sealed class UtBoolOperator(delegate: BoolOperator) : UtOperator<UtBoolOpExpression>(delegate) {
    override operator fun invoke(left: PrimitiveValue, right: PrimitiveValue): UtBoolOpExpression =
        UtBoolOpExpression(this, left, right)

    operator fun invoke(left: PrimitiveValue, right: Int): UtBoolOpExpression =
        UtBoolOpExpression(this, left, right.toPrimitiveValue())
}

object Le : UtBoolOperator(org.utbot.engine.z3.Le)
object Lt : UtBoolOperator(org.utbot.engine.z3.Lt)
object Ge : UtBoolOperator(org.utbot.engine.z3.Ge)
object Gt : UtBoolOperator(org.utbot.engine.z3.Gt)
object Eq : UtBoolOperator(org.utbot.engine.z3.Eq)
object Ne : UtBoolOperator(org.utbot.engine.z3.Ne)

object Rem : UtBinOperator(org.utbot.engine.z3.Rem)
object Div : UtBinOperator(org.utbot.engine.z3.Div)
object Mul : UtBinOperator(org.utbot.engine.z3.Mul)
object Add : UtBinOperator(org.utbot.engine.z3.Add)
object Sub : UtBinOperator(org.utbot.engine.z3.Sub)

object Shl : UtBinOperator(org.utbot.engine.z3.Shl, ::leftOperandType)
object Shr : UtBinOperator(org.utbot.engine.z3.Shr, ::leftOperandType)
object Ushr : UtBinOperator(org.utbot.engine.z3.Ushr, ::leftOperandType)

object Xor : UtBinOperator(org.utbot.engine.z3.Xor)
object Or : UtBinOperator(org.utbot.engine.z3.Or)
object And : UtBinOperator(org.utbot.engine.z3.And)

/**
 * NaN related logic - comparison anything with NaN gives false.
 * For that we have special instructions cmpl/cmpg.
 * If at least one of value1 or value2 is NaN, the result of the cmpg instruction is 1,
 * and the result of cmpl is -1.
 */
object Cmp : UtBinOperator(org.utbot.engine.z3.Cmp, ::intSort)
object Cmpl : UtBinOperator(org.utbot.engine.z3.Cmpl, ::intSort)
object Cmpg : UtBinOperator(org.utbot.engine.z3.Cmpg, ::intSort)

fun maxSort(left: PrimitiveValue, right: PrimitiveValue) =
    maxOf(left.expr.sort, right.expr.sort, UtIntSort, compareBy { it.rank() })

// Straight-forward maxSort when we don't want UtIntSort to be lesser limit
fun simpleMaxSort(left: PrimitiveValue, right: PrimitiveValue) =
    maxOf(left.expr.sort, right.expr.sort, compareBy { it.rank() })

@Suppress("UNUSED_PARAMETER")
private fun leftOperandType(left: PrimitiveValue, right: PrimitiveValue) = alignSort(left.expr.sort)

@Suppress("UNUSED_PARAMETER")
private fun intSort(left: PrimitiveValue, right: PrimitiveValue) = UtIntSort

/**
 * Calculates a rank for UtSort to use as a comparator.
 * Ranks them by type and bit size.
 * FPSort is the highest type.
 * Note: BoolSort is higher than BitVecSort to cast 0/1 constants to boolean.
 */
private fun UtSort.rank(): Int = when (this) {
    UtFp64Sort -> 30000000 + 64
    UtFp32Sort -> 30000000 + 32
    UtBoolSort -> 20000000
    is UtBvSort -> 10000000 + this.size
    else -> error("Wrong sort $this")
}