@file:Suppress("unused")

package org.utbot.engine.z3

import org.utbot.engine.pc.Z3Variable
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.FPExpr
import com.microsoft.z3.Sort
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.engine.z3.ExtensionsKtTest.Companion.toSort
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.ShortType
import soot.Type

internal class ExtensionsKtTest {
    @ParameterizedTest
    @MethodSource("convertVarArgs")
    fun testConvertVar(variable: Z3Variable, type: Type) {
        val expr = variable.expr
        when {
            // this fine, we don't have such conversion, so let's check fail here
            expr is FPExpr && type is BooleanType -> assertThrows<IllegalStateException> {
                context.convertVar(
                    variable,
                    type
                )
            }
            expr is BoolExpr && type !is BooleanType -> assertThrows<IllegalStateException> {
                context.convertVar(
                    variable,
                    type
                )
            }
            else -> {
                val alignVar = context.convertVar(variable, type)
                assertEquals(type, alignVar.type)
                assertEquals(context.toSort(type), alignVar.expr.sort)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("conversions")
    fun testConversions(conversion: Conversion) {
        with(conversion) {
            for ((i, number) in numbers.withIndex()) {
                testConversions(number, numbers.subList(i + 1, numbers.size))
            }
        }
    }

    @ParameterizedTest
    @MethodSource("fromCharArgs")
    fun testFromChar(from: Char, result: Number) {
        testCharJavaConversion(from, result)
        val variable = variable(from)
        val value = context.convertVar(variable, result.toType()).expr.simplify().value()
        when (result) {
            is Byte -> assertEquals(result, value as Byte) { "$from to $result" }
            is Short -> assertEquals(result, value as Short) { "$from to $result" }
            is Int -> assertEquals(result, value as Int) { "$from to $result" }
            is Long -> assertEquals(result, value as Long) { "$from to $result" }
            is Float -> assertEquals(result, value as Float) { "$from to $result" }
            is Double -> assertEquals(result, value as Double) { "$from to $result" }
        }
    }

    @ParameterizedTest
    @MethodSource("toCharArgs")
    fun testToChar(from: Number, result: Char) {
        assertEquals(result, from.toChar()) {
            "Java: $from (${
                from.toChar().prettify()
            }) to $result (${result.prettify()})"
        }
        val variable = variable(from)
        val value = context.convertVar(variable, CharType.v()).expr.simplify().value(true) as Char
        assertEquals(result, value) { "$from to $result" }
    }

    @ParameterizedTest
    @MethodSource("fromSolverStringArgs")
    fun testConvertFromSolver(from: String, to: String) {
        val converted = convertSolverString(from)
        assertEquals(to, converted) { "${to.codes()} != ${converted.codes()}" }
    }

    private fun String.codes() = map { it.toInt() }.joinToString()

    private fun testConversions(from: Number, to: List<Number>) {
        to.forEach { result ->
            testJavaConversion(from, result)
            val variable = variable(from)
            val value = context.convertVar(variable, result.toType()).expr.simplify().value()
            when (result) {
                is Byte -> assertEquals(result, value as Byte) { "$from to $result" }
                is Short -> assertEquals(result, value as Short) { "$from to $result" }
                is Int -> assertEquals(result, value as Int) { "$from to $result" }
                is Long -> assertEquals(result, value as Long) { "$from to $result" }
                is Float -> assertEquals(result, value as Float) { "$from to $result" }
                is Double -> assertEquals(result, value as Double) { "$from to $result" }
            }
        }
    }

    private fun testJavaConversion(from: Number, result: Number) {
        when (result) {
            is Byte -> assertEquals(result, from.toByte()) { "Java: $from to $result" }
            is Short -> assertEquals(result, from.toShort()) { "Java: $from to $result" }
            is Int -> assertEquals(result, from.toInt()) { "Java: $from to $result" }
            is Long -> assertEquals(result, from.toLong()) { "Java: $from to $result" }
            is Float -> assertEquals(result, from.toFloat()) { "Java: $from to $result" }
            is Double -> assertEquals(result, from.toDouble()) { "Java: $from to $result" }
        }
    }

    private fun testCharJavaConversion(from: Char, result: Number) {
        when (result) {
            is Byte -> assertEquals(result, from.toByte()) { "Java: $from to $result" }
            is Short -> assertEquals(result, from.toShort()) { "Java: $from to $result" }
            is Int -> assertEquals(result, from.toInt()) { "Java: $from to $result" }
            is Long -> assertEquals(result, from.toLong()) { "Java: $from to $result" }
            is Float -> assertEquals(result, from.toFloat()) { "Java: $from to $result" }
            is Double -> assertEquals(result, from.toDouble()) { "Java: $from to $result" }
        }
    }

    companion object : Z3Initializer() {
        @JvmStatic
        fun convertVarArgs() =
            series.flatMap { left -> series.map { right -> arguments(left, right.type) } }

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

        /**
         * Arguments for conversion checks.
         *
         * How to read conversion line: from left to right each number can be converted to all following,
         * so Conversion(100.toByte(), 100.toShort(), 100, 100L) means it checks:
         * - byte to short, int, long
         * - short to int, long
         * - int to long
         *
         * Notes:
         * - char primitive class is missing with its conversions
         *
         * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.1.2">
         * Java Language Specification</a>
         */
        @JvmStatic
        fun conversions() = listOf(
            // A widening, from an integral type to another integral type, sign-extends
            Conversion((-100).toByte(), (-100).toShort(), -100, -100L),
            Conversion(100.toByte(), 100.toShort(), 100, 100L),

            /**
             * A narrowing conversion may lose information about the overall magnitude of a numeric value
             * and may also lose precision and range.
             * Integral: simply discards all but the n lowest order bits
             */
            Conversion(-100L, -100, (-100).toShort(), (-100).toByte()),
            Conversion(0x1111222233334444L, 0x33334444, (0x4444).toShort(), (0x44).toByte()),
            Conversion(4000000012L, -294967284, 10252.toShort(), 12.toByte()),
            Conversion(-1L, -1, (-1).toShort(), (-1).toByte()),

            // Int to float, long to double, may lose some of the least significant bits of the value
            Conversion(1234567890, 1.23456794E9f),
            Conversion(-1234567890, -1.23456794E9f),
            Conversion(6000372036854775807L, 6.0003720368547758E18),
            Conversion(-6000372036854775807L, -6.0003720368547758E18),

            /**
             * Double to float, narrowing.
             * A finite value too small to be represented as a float is converted to a zero of the same sign;
             * a finite value too large to be represented as a float is converted to an infinity of the same sign.
             * A double NaN is converted to a float NaN.
             */
            Conversion(100.0, 100.0f),
            Conversion(-100.0, -100.0f),
            Conversion(Double.NaN, Float.NaN),
            Conversion(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            Conversion(Double.MAX_VALUE, Float.POSITIVE_INFINITY),
            Conversion(1E40, Float.POSITIVE_INFINITY),
            Conversion(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY),
            Conversion(-Double.MAX_VALUE, Float.NEGATIVE_INFINITY),
            Conversion(-1E40, Float.NEGATIVE_INFINITY),
            Conversion(0.0, 0.0f),
            Conversion(-0.0, -0.0f),
            Conversion(Double.MIN_VALUE, 0.0f),
            Conversion(-Double.MIN_VALUE, -0.0f),

            /**
             * Float to double, widening.
             * A float infinity is converted to a double infinity of the same sign.
             * A float NaN is converted to a double NaN.
             */
            Conversion(100.0f, 100.0),
            Conversion(-100.0f, -100.0),
            Conversion(Float.NaN, Double.NaN),
            Conversion(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
            Conversion(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
            Conversion(0.0f, 0.0),
            Conversion(-0.0f, -0.0),

            // float/double to long
            Conversion(100.999, 100L),
            Conversion(-100.999, -100L),
            Conversion(0.0, 0L),
            Conversion(-0.0, 0L),
            Conversion(Double.MIN_VALUE, 0L),
            Conversion(-Double.MIN_VALUE, 0L),
            Conversion(100.999f, 100L),
            Conversion(-100.999f, -100L),
            Conversion(0.0f, 0L),
            Conversion(-0.0f, -0L),

            // Special cases
            Conversion(Float.NaN, 0L),
            Conversion(Float.POSITIVE_INFINITY, Long.MAX_VALUE),
            Conversion(Float.NEGATIVE_INFINITY, Long.MIN_VALUE),
            Conversion(Double.NaN, 0L),
            Conversion(Double.POSITIVE_INFINITY, Long.MAX_VALUE),
            Conversion(Double.MAX_VALUE, Long.MAX_VALUE),
            Conversion(1E40, Long.MAX_VALUE),
            Conversion(Double.NEGATIVE_INFINITY, Long.MIN_VALUE),
            Conversion(-Double.MAX_VALUE, Long.MIN_VALUE),
            Conversion(-1E40, Long.MIN_VALUE),

            // float/double to int
            Conversion(100.999, 100),
            Conversion(-100.999, -100),
            Conversion(0.0, 0),
            Conversion(-0.0, 0),
            Conversion(Double.MIN_VALUE, 0),
            Conversion(-Double.MIN_VALUE, 0),
            Conversion(100.999f, 100),
            Conversion(-100.999f, -100),
            Conversion(0.0f, 0),
            Conversion(-0.0f, -0),

            // Special cases
            Conversion(Float.NaN, 0),
            Conversion(Float.POSITIVE_INFINITY, Int.MAX_VALUE),
            Conversion(Float.NEGATIVE_INFINITY, Int.MIN_VALUE),
            Conversion(Double.NaN, 0),
            Conversion(Double.POSITIVE_INFINITY, Int.MAX_VALUE),
            Conversion(Double.MAX_VALUE, Int.MAX_VALUE),
            Conversion(1E40, Int.MAX_VALUE),
            Conversion(Double.NEGATIVE_INFINITY, Int.MIN_VALUE),
            Conversion(-Double.MAX_VALUE, Int.MIN_VALUE),
            Conversion(-1E40, Int.MIN_VALUE),

            // float/double to byte (through int)
            Conversion(100.999, 100.toByte()),
            Conversion(-100.999, (-100).toByte()),
            Conversion(0.0, 0.toByte()),
            Conversion(-0.0, 0.toByte()),
            Conversion(Double.MIN_VALUE, 0.toByte()),
            Conversion(-Double.MIN_VALUE, 0.toByte()),
            Conversion(100.999f, 100.toByte()),
            Conversion(-100.999f, (-100).toByte()),
            Conversion(0.0f, 0.toByte()),
            Conversion(-0.0f, (-0).toByte()),

            // Special cases
            Conversion(Float.NaN, 0.toByte()),
            Conversion(Float.POSITIVE_INFINITY, (-1).toByte()), // narrowing from int to byte
            Conversion(Float.NEGATIVE_INFINITY, 0.toByte()), // narrowing from int to byte
            Conversion(Double.NaN, 0.toByte()),
            Conversion(Double.POSITIVE_INFINITY, (-1).toByte()), // narrowing from int to byte
            Conversion(Double.MAX_VALUE, (-1).toByte()), // narrowing from int to byte
            Conversion(1E40, (-1).toByte()), // narrowing from int to byte
            Conversion(Double.NEGATIVE_INFINITY, 0.toByte()), // narrowing from int to byte
            Conversion(-Double.MAX_VALUE, 0.toByte()), // narrowing from int to byte
            Conversion(-1E40, 0.toByte()), // narrowing from int to byte
        )

        @JvmStatic
        fun fromCharArgs() = listOf(
            arguments('\u9999', (-103).toByte()),
            arguments('\u9999', (-26215).toShort()),
            arguments('\u9999', 39321),
            arguments('\u9999', 39321L),
            arguments('\u9999', 39321.0f),
            arguments('\u9999', 39321.0),
        )

        @JvmStatic
        fun toCharArgs() = listOf(
            arguments((-103).toByte(), '\uFF99'),
            arguments(103.toByte(), '\u0067'),
            arguments((-26215).toShort(), '\u9999'),
            arguments(26215.toShort(), '\u6667'),
            arguments(1234567890, '\u02D2'),
            arguments(-1234567890, '\uFD2E'),
            arguments(6000372036854775807L, '\u7FFF'),
            arguments(-6000372036854775807L, '\u8001'),

            // float/double to char (through int)
            arguments(-39321.0, '\u6667'),
            arguments(100.999, '\u0064'),
            arguments(-100.999, '\uFF9C'),
            arguments(0.0, '\u0000'),
            arguments(-0.0, '\u0000'),
            arguments(Double.MIN_VALUE, '\u0000'),
            arguments(-Double.MIN_VALUE, '\u0000'),
            arguments(-39321.0f, '\u6667'),
            arguments(100.999f, '\u0064'),
            arguments(-100.999f, '\uFF9C'),
            arguments(0.0f, '\u0000'),
            arguments(-0.0f, '\u0000'),

            // Special cases
            arguments(Float.NaN, '\u0000'),
            arguments(Float.POSITIVE_INFINITY, '\uFFFF'), // narrowing from int to char
            arguments(Float.NEGATIVE_INFINITY, '\u0000'), // narrowing from int to char
            arguments(Double.NaN, '\u0000'),
            arguments(Double.POSITIVE_INFINITY, '\uFFFF'), // narrowing from int to char
            arguments(Double.MAX_VALUE, '\uFFFF'), // narrowing from int to char
            arguments(1E40, '\uFFFF'), // narrowing from int to char
            arguments(Double.NEGATIVE_INFINITY, '\u0000'), // narrowing from int to char
            arguments(-Double.MAX_VALUE, '\u0000'), // narrowing from int to char
            arguments(-1E40, '\u0000'), // narrowing from int to char
        )

        @JvmStatic
        fun fromSolverStringArgs() = listOf(
            arguments("", ""),
            arguments("\\a", "\u0007"),
            arguments("\\b", "\b"),
            arguments("\\f", "\u000C"),
            arguments("\\n", "\n"),
            arguments("\\r", "\r"),
            arguments("\\t", "\t"),
            arguments("\\v", "\u000B"),

            arguments("\\x00", "\u0000"),
            arguments("\\xAB", "\u00AB"),
            arguments("AaBbCc (){}[]<>,.!@#$%^&*-=_+/?`~\\\\", "AaBbCc (){}[]<>,.!@#$%^&*-=_+/?`~\\"),
        )

        private fun variable(from: Any): Z3Variable = when (from) {
            is Number -> Z3Variable(from.toType(), context.makeConst(from, from.toSort()))
            is Char -> Z3Variable(CharType.v(), context.mkBV(from.toInt(), Char.SIZE_BITS))
            else -> error("Unknown constant: ${from::class}")
        }

        private fun Any.toSort(): Sort = when (this) {
            is Byte -> context.mkBitVecSort(Byte.SIZE_BITS)
            is Short -> context.mkBitVecSort(Short.SIZE_BITS)
            is Char -> context.mkBitVecSort(Char.SIZE_BITS)
            is Int -> context.mkBitVecSort(Int.SIZE_BITS)
            is Long -> context.mkBitVecSort(Long.SIZE_BITS)
            is Float -> context.mkFPSort32()
            is Double -> context.mkFPSort64()
            else -> error("Unknown type $this")
        }

        private fun Any.toType(): Type = when (this) {
            is Byte -> ByteType.v()
            is Short -> ShortType.v()
            is Char -> CharType.v()
            is Int -> IntType.v()
            is Long -> LongType.v()
            is Float -> FloatType.v()
            is Double -> DoubleType.v()
            else -> error("Unknown type $this")
        }
    }
}

private fun Char.prettify(): String = "%04X".format(this.toInt()).let { "'\\u$it'" }

data class Conversion(val numbers: List<Number>) {
    constructor(vararg numbers: Number) : this(numbers.toList())
}