@file:Suppress("PackageDirectoryMismatch")

package com.microsoft.z3

import java.lang.Integer.parseUnsignedInt
import java.lang.Long.parseUnsignedLong

fun Model.eval(expr: Expr): Expr = this.eval(expr, true)

fun BitVecNum.toIntNum(unsigned: Boolean = false): Any = when (sortSize) {
    Byte.SIZE_BITS -> parseUnsignedLong(this.toBinaryString(), 2).toByte()
    Short.SIZE_BITS -> if (unsigned) {
        parseUnsignedLong(this.toBinaryString(), 2).toChar()
    } else {
        parseUnsignedLong(this.toBinaryString(), 2).toShort()
    }
    Int.SIZE_BITS -> parseUnsignedLong(this.toBinaryString(), 2).toInt()
    Long.SIZE_BITS -> parseUnsignedLong(this.toBinaryString(), 2)
    // fallback for others
    else -> parseUnsignedLong(this.toBinaryString(), 2).toInt()
}

fun FPNum.toFloatingPointNum(): Number = when (sort) {
    context.mkFPSort64() -> when {
        this.isNaN -> Double.NaN
        this.isInf && this.isPositive -> Double.POSITIVE_INFINITY
        this.isInf && this.isNegative -> Double.NEGATIVE_INFINITY
        else -> {
            val bitVecNum = context.mkFPToIEEEBV(this).simplify() as BitVecNum

            Double.fromBits(parseUnsignedLong(bitVecNum.toBinaryString(), 2))
        }
    }
    context.mkFPSort32() -> when {
        this.isNaN -> Float.NaN
        this.isInf && this.isPositive -> Float.POSITIVE_INFINITY
        this.isInf && this.isNegative -> Float.NEGATIVE_INFINITY
        else -> {
            val bitVecNum = context.mkFPToIEEEBV(this).simplify() as BitVecNum

            Float.fromBits(parseUnsignedInt(bitVecNum.toBinaryString(), 2))
        }
    }
    else -> error("Unknown type: $sort")
}

fun Context.mkSeqNth(s: SeqExpr, index: Expr): Expr {
    this.checkContextMatch(s, index)
    return Expr.create(this, Native.mkSeqNth(nCtx(), s.nativeObject, index.nativeObject))
}