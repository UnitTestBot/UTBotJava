package org.utbot.engine.z3

import org.utbot.engine.pc.Z3Variable
import org.utbot.engine.pc.z3Variable
import com.microsoft.z3.BitVecExpr
import com.microsoft.z3.BitVecNum
import com.microsoft.z3.BitVecSort
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.BoolSort
import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.FPExpr
import com.microsoft.z3.FPNum
import com.microsoft.z3.FPSort
import com.microsoft.z3.SeqExpr
import com.microsoft.z3.Sort
import com.microsoft.z3.enumerations.Z3_lbool
import com.microsoft.z3.toFloatingPointNum
import com.microsoft.z3.toIntNum
import java.lang.Integer.parseInt
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefType
import soot.ShortType
import soot.Type

private val Z3Variable.unsigned: Boolean
    get() = this.type is CharType

fun Expr.value(unsigned: Boolean = false): Any = when (this) {
    is BitVecNum -> this.toIntNum(unsigned)
    is FPNum -> this.toFloatingPointNum()
    is BoolExpr -> this.boolValue == Z3_lbool.Z3_L_TRUE
    is SeqExpr -> convertSolverString(this.string)
    else -> error("${this::class}: $this")
}

internal fun Expr.intValue() = this.value() as Int

/**
 * Converts a variable to given type.
 *
 * Note: special case "byte to char":
 * First, the byte is converted to an int via widening primitive conversion (§5.1.2), and then
 * the resulting int is converted to a char by narrowing primitive conversion (§5.1.3).
 *
 * @throws IllegalStateException if given expression cannot be convert to given sort
 */
fun Context.convertVar(variable: Z3Variable, toType: Type): Z3Variable {
    if (toType == variable.type) return variable
    if (variable.type is ByteType && toType is CharType) {
        return convertVar(convertVar(variable, IntType.v()), toType)
    }
    return convertVar(variable, toSort(toType)).z3Variable(toType)
}

/**
 * Converts an expression from variable to given sort.
 *
 * @throws IllegalStateException if given expression cannot be convert to given sort
 */
internal fun Context.convertVar(variable: Z3Variable, sort: Sort): Expr {
    val expr = variable.expr
    return when {
        sort == expr.sort -> expr
        sort is FPSort && expr is FPExpr -> mkFPToFP(mkFPRoundNearestTiesToEven(), expr, sort)
        sort is FPSort && expr is BitVecExpr -> mkFPToFP(mkFPRoundNearestTiesToEven(), expr, sort, !variable.unsigned)
        sort is BitVecSort && expr is FPExpr -> convertFPtoBV(variable, sort.size)
        sort is BoolSort && expr is BitVecExpr -> Ne(this, variable, 0)
        sort is BitVecSort && expr is BitVecExpr -> {
            val diff = sort.size - expr.sortSize
            if (diff > 0) {
                if (variable.unsigned) {
                    mkZeroExt(diff, expr)
                } else {
                    mkSignExt(diff, expr)
                }
            } else {
                mkExtract(sort.size - 1, 0, expr)
            }
        }
        else -> error("Wrong expr $expr and sort $sort")
    }
}

/**
 * Converts FP to BV covering Java logic:
 * - convert to long for long
 * - convert to int for other types, with second step conversion by taking lowest bits
 * (can lead to significant changes if value outside of target type range).
 */
private fun Context.convertFPtoBV(variable: Z3Variable, sortSize: Int): Expr = when (sortSize) {
    Long.SIZE_BITS -> convertFPtoBV(variable, sortSize, Long.MIN_VALUE, Long.MAX_VALUE)
    else -> doNarrowConversion(
        convertFPtoBV(variable, Int.SIZE_BITS, Int.MIN_VALUE, Int.MAX_VALUE) as BitVecExpr,
        sortSize
    )
}

/**
 * Converts FP to BV covering special cases for NaN, Infinity and values outside of [minValue, maxValue] range.
 */
private fun Context.convertFPtoBV(variable: Z3Variable, sortSize: Int, minValue: Number, maxValue: Number): Expr =
    mkITE(
        mkFPIsNaN(variable.expr as FPExpr), makeConst(0, mkBitVecSort(sortSize)),
        mkITE(
            Lt(this, variable, minValue), makeConst(minValue, mkBitVecSort(sortSize)),
            mkITE(
                Gt(this, variable, maxValue), makeConst(maxValue, mkBitVecSort(sortSize)),
                mkFPToBV(mkFPRoundTowardZero(), variable.expr, sortSize, true)
            )
        )
    )

/**
 * Converts int to byte, short or char.
 */
internal fun Context.doNarrowConversion(expr: BitVecExpr, sortSize: Int): BitVecExpr = when (sortSize) {
    expr.sortSize -> expr
    else -> mkExtract(sortSize - 1, 0, expr)
}

/**
 * Converts a constant value to Z3 constant of given sort - BitVec or FP.
 */
fun Context.makeConst(const: Number, sort: Sort): Expr = when (sort) {
    is BitVecSort -> this.makeBV(const, sort.size)
    is FPSort -> this.makeFP(const, sort)
    else -> error("Wrong sort $sort")
}

fun Context.makeBV(const: Number, size: Int): BitVecExpr = when (const) {
    is Byte -> mkBV(const.toInt(), size)
    is Short -> mkBV(const.toInt(), size)
    is Int -> mkBV(const, size)
    is Long -> mkBV(const, size)
    else -> error("Wrong type ${const::class}")
}

fun Context.makeFP(const: Number, sort: FPSort): FPExpr = when (const) {
    is Float -> mkFP(const, sort)
    is Double -> mkFP(const, sort)
    is Int -> mkFP(const, sort)
    is Long -> mkFPToFP(mkFPRoundNearestTiesToEven(), mkBV(const, Long.SIZE_BITS), sort, true)
    else -> error("Wrong type ${const::class}")
}

fun Context.toSort(type: Type): Sort =
    when (type) {
        is ByteType -> mkBitVecSort(Byte.SIZE_BITS)
        is ShortType -> mkBitVecSort(Short.SIZE_BITS)
        is CharType -> mkBitVecSort(Char.SIZE_BITS)
        is IntType -> mkBitVecSort(Int.SIZE_BITS)
        is LongType -> mkBitVecSort(Long.SIZE_BITS)
        is FloatType -> mkFPSort32()
        is DoubleType -> mkFPSort64()
        is BooleanType -> mkBoolSort()
        is RefType -> mkBitVecSort(Int.SIZE_BITS)
        else -> error("${type::class} sort is not implemented")
    }

fun convertSolverString(s: String) = buildString(s.length) {
    var i = 0
    while (i < s.length) {
        when (val char = s[i]) {
            '\\' -> {
                i++
                if (i >= s.length) error("Escape symbol without escaped")
                when (s[i]) {
                    'a' -> append('\u0007')
                    'b' -> append('\b')
                    'f' -> append('\u000C')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    'v' -> append('\u000B')
                    '\\' -> append('\\')
                    'x' -> {
                        if (i + 2 >= s.length) error("Wrong sequence of \\x")
                        val hex = parseInt(s.substring(i + 1, i + 3), 16)
                        append(hex.toChar())
                        i += 2
                    }
                }
            }
            else -> append(char)
        }
        i++
    }
}