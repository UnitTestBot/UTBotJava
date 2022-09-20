@file:Suppress("unused")

package org.utbot.engine.z3

import org.utbot.engine.pc.Z3Variable
import com.microsoft.z3.BitVecSort
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.FPExpr
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.ShortType

internal class OperatorsKtTest {
    @ParameterizedTest
    @MethodSource("alignVarsArgs")
    fun testAlignVars(left: Z3Variable, right: Z3Variable) {
        if (left.expr is BoolExpr && right.expr is FPExpr || left.expr is FPExpr && right.expr is BoolExpr) {
            // this fine, we don't have such conversion, so let's check fail here
            assertThrows<IllegalStateException> { context.alignVars(left, right) }
        } else {
            val (aleft, aright) = context.alignVars(left, right)
            // Binary numeric promotion - byte, short and char converted to int before operation
            if (left.expr.sort is BitVecSort && aleft.sort is BitVecSort) {
                assertTrue((aleft.sort as BitVecSort).size >= Int.SIZE_BITS)
            }
            if (right.expr.sort is BitVecSort && aright.sort is BitVecSort) {
                assertTrue((aright.sort as BitVecSort).size >= Int.SIZE_BITS)
            }
            assertEquals(aleft.sort, aright.sort)
        }
    }

    @ParameterizedTest
    @MethodSource("alignVarArgs")
    fun testAlignVar(variable: Z3Variable) {
        val aligned = context.alignVar(variable)
        // Unary numeric promotion - byte, short and char converted to int before operation
        if (variable.expr.sort is BitVecSort && (variable.expr.sort as BitVecSort).size < Int.SIZE_BITS) {
            assertTrue((aligned.expr.sort as BitVecSort).size == Int.SIZE_BITS)
        } else {
            assertEquals(variable, aligned)
        }
    }

    companion object : Z3Initializer() {
        @JvmStatic
        fun alignVarsArgs() =
            series.flatMap { left -> series.map { right -> arguments(left, right) } }

        @JvmStatic
        fun alignVarArgs() = series

        private val series = listOf(
            Z3Variable(ByteType.v(), context.mkBV(Random.nextInt(0, 100), Byte.SIZE_BITS)),
            Z3Variable(ShortType.v(), context.mkBV(Random.nextInt(0, 100), Short.SIZE_BITS)),
            Z3Variable(CharType.v(), context.mkBV(Random.nextInt(50000, 60000), Char.SIZE_BITS)),
            Z3Variable(IntType.v(), context.mkBV(Random.nextInt(0, 100), Int.SIZE_BITS)),
            Z3Variable(LongType.v(), context.mkBV(Random.nextInt(0, 100), Long.SIZE_BITS)),
            Z3Variable(FloatType.v(), context.mkFP(Random.nextFloat(), context.mkFPSort32())),
            Z3Variable(DoubleType.v(), context.mkFP(Random.nextDouble(), context.mkFPSort64())),
            Z3Variable(BooleanType.v(), context.mkBool(Random.nextBoolean()))
        )
    }
}