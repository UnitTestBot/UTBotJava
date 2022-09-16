package org.utbot.engine.z3

import org.utbot.engine.pc.Z3Variable
import com.microsoft.z3.BitVecExpr
import com.microsoft.z3.BitVecSort
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.BoolSort
import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.FPExpr
import com.microsoft.z3.FPSort
import com.microsoft.z3.Sort
import soot.ByteType
import soot.CharType
import soot.IntType
import soot.ShortType

typealias BinOperator = Operator<out Expr>

sealed class Operator<T : Expr>(
    val onBitVec: (Context, BitVecExpr, BitVecExpr) -> T,
    val onFP: (Context, FPExpr, FPExpr) -> T = { _, _, _ -> TODO() },
    val onBool: (Context, BoolExpr, BoolExpr) -> T = { _, _, _ -> TODO() }
) {
    open operator fun invoke(context: Context, left: Z3Variable, right: Z3Variable): T =
        context.alignVars(left, right).let { (aleft, aright) ->
            doAction(context, aleft, aright)
        }

    open operator fun invoke(context: Context, left: Z3Variable, right: Number): T =
        context.alignVarAndConst(left, right).let { (aleft, aright) ->
            doAction(context, aleft, aright)
        }

    protected fun doAction(context: Context, left: Expr, right: Expr): T = when {
        left is BitVecExpr && right is BitVecExpr -> onBitVec(context, left, right)
        left is FPExpr && right is FPExpr -> onFP(context, left, right)
        left is BoolExpr && right is BoolExpr -> onBool(context, left, right)
        else -> error("Not implemented $this for $left, $right")
    }

    override fun toString(): String = this.javaClass.simpleName
}

abstract class BoolOperator(
    onBitVec: (Context, BitVecExpr, BitVecExpr) -> BoolExpr,
    onFP: (Context, FPExpr, FPExpr) -> BoolExpr,
    onBool: (Context, BoolExpr, BoolExpr) -> BoolExpr = { _, _, _ -> TODO() }
) : Operator<BoolExpr>(onBitVec, onFP, onBool)

object Le : BoolOperator(Context::mkBVSLE, Context::mkFPLEq)
object Lt : BoolOperator(Context::mkBVSLT, Context::mkFPLt)
object Ge : BoolOperator(Context::mkBVSGE, Context::mkFPGEq)
object Gt : BoolOperator(Context::mkBVSGT, Context::mkFPGt)
object Eq : BoolOperator(Context::mkEq, Context::mkFPEq, Context::mkEq)
object Ne : BoolOperator(::ne, ::fpNe, ::ne)

private fun ne(context: Context, left: Expr, right: Expr): BoolExpr = context.mkNot(context.mkEq(left, right))
private fun fpNe(context: Context, left: FPExpr, right: FPExpr): BoolExpr = context.mkNot(context.mkFPEq(left, right))

internal object Rem : BinOperator(Context::mkBVSRem, Context::mkFPRem)
internal object Div : BinOperator(Context::mkBVSDiv, ::fpDiv)
internal object Mul : BinOperator(Context::mkBVMul, ::fpMul)
internal object Add : BinOperator(Context::mkBVAdd, ::fpAdd)
internal object Sub : BinOperator(Context::mkBVSub, ::fpSub)

private fun fpDiv(context: Context, left: FPExpr, right: FPExpr): FPExpr =
    context.mkFPDiv(context.mkFPRoundNearestTiesToEven(), left, right)

private fun fpMul(context: Context, left: FPExpr, right: FPExpr): FPExpr =
    context.mkFPMul(context.mkFPRoundNearestTiesToEven(), left, right)

private fun fpAdd(context: Context, left: FPExpr, right: FPExpr): FPExpr =
    context.mkFPAdd(context.mkFPRoundNearestTiesToEven(), left, right)

private fun fpSub(context: Context, left: FPExpr, right: FPExpr): FPExpr =
    context.mkFPSub(context.mkFPRoundNearestTiesToEven(), left, right)

/**
 * Shift operator.
 * Unary numeric promotion (§5.6.1) is performed on each operand separately.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.19">
 * Java Language Specification: Shift Operators</a>
 */
internal abstract class ShiftOperator(
    onBitVec: (Context, BitVecExpr, BitVecExpr) -> BitVecExpr
) : Operator<BitVecExpr>(onBitVec) {
    override fun invoke(context: Context, left: Z3Variable, right: Z3Variable): BitVecExpr {
        val aleft = context.alignVar(left).expr as BitVecExpr
        val aright = context.convertVar(right, aleft.sort) as BitVecExpr
        val mask = mask(aleft)
        val shift = context.mkBVAND(aright, context.mkBV(mask, aleft.sortSize))
        return doAction(context, aleft, shift)
    }

    override fun invoke(context: Context, left: Z3Variable, right: Number): BitVecExpr {
        val aleft = context.alignVar(left).expr as BitVecExpr
        val mask = mask(aleft)
        val shift = right.toLong() and mask
        return doAction(context, aleft, context.mkBV(shift, aleft.sortSize))
    }

    private fun mask(expr: BitVecExpr): Long = when (expr.sortSize) {
        Int.SIZE_BITS -> 0b11111
        Long.SIZE_BITS -> 0b111111
        else -> error("Unknown sort for $expr")
    }
}

internal object Shl : ShiftOperator(Context::mkBVSHL)
internal object Shr : ShiftOperator(Context::mkBVASHR)
internal object Ushr : ShiftOperator(Context::mkBVLSHR)

internal object Xor : BinOperator(Context::mkBVXOR, onBool = Context::mkXor)
internal object Or : BinOperator(Context::mkBVOR, onBool = ::boolOr)
internal object And : BinOperator(Context::mkBVAND, onBool = ::boolAnd)

private fun boolOr(context: Context, left: BoolExpr, right: BoolExpr): BoolExpr = context.mkOr(left, right)
private fun boolAnd(context: Context, left: BoolExpr, right: BoolExpr): BoolExpr = context.mkAnd(left, right)

/**
 * NaN related logic - comparison anything with NaN gives false.
 * For that we have special instructions cmpl/cmpg.
 * If at least one of value1 or value2 is NaN, the result of the cmpg instruction is 1,
 * and the result of cmpl is -1.
 */
internal object Cmp : BinOperator(::bvCmp, ::fpCmp)
internal object Cmpl : BinOperator(::bvCmp, ::fpCmpl)
internal object Cmpg : BinOperator(::bvCmp, ::fpCmpg)

private fun bvCmp(context: Context, left: BitVecExpr, right: BitVecExpr): Expr =
    context.mkITE(
        context.mkBVSLE(left, right),
        context.mkITE(context.mkEq(left, right), context.mkBV(0, 32), context.mkBV(-1, 32)),
        context.mkBV(1, 32)
    )

/**
 * Perhaps nobody uses it, cause for FP it's always cmpl/cmpg.
 */
private fun fpCmp(context: Context, left: FPExpr, right: FPExpr) =
    context.mkITE(
        context.mkFPLEq(left, right),
        context.mkITE(context.mkFPEq(left, right), context.mkBV(0, 32), context.mkBV(-1, 32)),
        context.mkBV(1, 32)
    )

private fun fpCmpl(context: Context, left: FPExpr, right: FPExpr): Expr =
    context.mkITE(
        context.mkOr(context.mkFPIsNaN(left), context.mkFPIsNaN(right)), context.mkBV(-1, 32),
        context.mkITE(
            context.mkFPLEq(left, right),
            context.mkITE(context.mkFPEq(left, right), context.mkBV(0, 32), context.mkBV(-1, 32)),
            context.mkBV(1, 32)
        )
    )

private fun fpCmpg(context: Context, left: FPExpr, right: FPExpr): Expr =
    context.mkITE(
        context.mkOr(context.mkFPIsNaN(left), context.mkFPIsNaN(right)), context.mkBV(1, 32),
        context.mkITE(
            context.mkFPLEq(left, right),
            context.mkITE(context.mkFPEq(left, right), context.mkBV(0, 32), context.mkBV(-1, 32)),
            context.mkBV(1, 32)
        )
    )

fun negate(context: Context, variable: Z3Variable): Expr = when (variable.expr) {
    is BitVecExpr -> context.mkBVNeg(context.alignVar(variable).expr as BitVecExpr)
    is FPExpr -> context.mkFPNeg(variable.expr)
    is BoolExpr -> context.mkNot(variable.expr)
    else -> error("Unsupported expr: ${variable.expr}")
}

/**
 * Aligns two variables to one Sort for do an operation on them.
 * We find higher Sort for them and align both to it.
 *
 * Note: according to Binary Numeric Promotion byte, short and char converted to int before operation, so then
 * we don't have unsigned operands.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.6.2">
 * Java Language Specification: Binary Numeric Promotion</a>
 */
fun Context.alignVars(left: Z3Variable, right: Z3Variable): Pair<Expr, Expr> {
    val maxSort = maxOf(left.expr.sort, right.expr.sort, mkBitVecSort(Int.SIZE_BITS), compareBy { it.rank() })
    return convertVar(left, maxSort) to convertVar(right, maxSort)
}

/**
 * Aligns a variable and a constant to one Sort for do an operation on them.
 * We find higher Sort for them and align both to it.
 *
 * Note: according to Binary Numeric Promotion byte, short and char converted to int before operation, so then
 * we don't have unsigned operands.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.6.2">
 * Java Language Specification: Binary Numeric Promotion</a>
 */
internal fun Context.alignVarAndConst(left: Z3Variable, right: Number): Pair<Expr, Expr> {
    val maxSort = maxOf(left.expr.sort, toSort(right), mkBitVecSort(Int.SIZE_BITS), compareBy { it.rank() })
    return convertVar(left, maxSort) to makeConst(right, maxSort)
}

/**
 * Implements Unary Numeric Promotion.
 * Converts byte, short and char to int.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.6.1">
 * Java Language Specification: Unary Numeric Promotion</a>
 */
fun Context.alignVar(variable: Z3Variable): Z3Variable = when (variable.type) {
    is ByteType, is ShortType, is CharType -> convertVar(variable, IntType.v())
    else -> variable
}

/**
 * Calculates a rank for Sort to use as a comparator.
 * Ranks them by type and bit size.
 * FPSort is the highest type.
 * Note: BoolSort is higher than BitVecSort to cast 0/1 constants to boolean.
 */
private fun Sort.rank(): Int = when (this) {
    is FPSort -> 30000000 + this.eBits + this.sBits
    is BoolSort -> 20000000
    is BitVecSort -> 10000000 + this.size
    else -> error("Wrong sort $this")
}

private fun Context.toSort(value: Number): Sort =
    when (value) {
        is Byte -> mkBitVecSort(Byte.SIZE_BITS)
        is Short -> mkBitVecSort(Short.SIZE_BITS)
        is Int -> mkBitVecSort(Int.SIZE_BITS)
        is Long -> mkBitVecSort(Long.SIZE_BITS)
        is Float -> mkFPSort32()
        is Double -> mkFPSort64()
        else -> error("${value::class} sort is not implemented")
    }