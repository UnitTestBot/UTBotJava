package org.utbot.examples.arrays

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class PrimitiveArraysTest : UtValueTestCaseChecker(
    testClass = PrimitiveArrays::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testDefaultIntValues() {
        check(
            PrimitiveArrays::defaultIntValues,
            eq(1),
            { r -> r != null && r.all { it == 0 } },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testDefaultDoubleValues() {
        check(
            PrimitiveArrays::defaultDoubleValues,
            eq(1),
            { r -> r != null && r.all { it == 0.0 } },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testDefaultBooleanValues() {
        check(
            PrimitiveArrays::defaultBooleanValues,
            eq(1),
            { r -> r != null && r.none { it } },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testByteArray() {
        checkWithException(
            PrimitiveArrays::byteArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 <= 20 && r.getOrNull() == 0.toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20 && r.getOrNull() == 1.toByte() }
        )
    }

    @Test
    fun testShortArray() {
        checkWithException(
            PrimitiveArrays::shortArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 <= 20 && r.getOrNull() == 0.toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20 && r.getOrNull() == 1.toByte() }
        )
    }

    @Test
    fun testCharArray() {
        checkWithException(
            PrimitiveArrays::charArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 <= 20.toChar() && r.getOrNull() == 0.toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20.toChar() && r.getOrNull() == 1.toByte() }
        )
    }

    @Test
    fun testIntArray() {
        checkWithException(
            PrimitiveArrays::intArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 <= 20 && r.getOrNull() == 0.toByte() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20 && r.getOrNull() == 1.toByte() }
        )
    }

    @Test
    fun testLongArray() {
        checkWithException(
            PrimitiveArrays::longArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toLong() },
            { a, x, r -> a != null && a.size == 2 && x + 5 <= 20 && r.getOrNull() == 0.toLong() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20 && r.getOrNull() == 1.toLong() }
        )
    }

    @Suppress("SimplifyNegatedBinaryExpression")
    @Test
    fun testFloatArray() {
        checkWithException(
            PrimitiveArrays::floatArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toFloat() },
            { a, x, r -> a != null && a.size == 2 && !(x + 5 > 20) && r.getOrNull() == 0.toFloat() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20 && r.getOrNull() == 1.toFloat() }
        )
    }

    @Suppress("SimplifyNegatedBinaryExpression")
    @Test
    fun testDoubleArray() {
        checkWithException(
            PrimitiveArrays::doubleArray,
            eq(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 2 && r.getOrNull() == (-1).toDouble() },
            { a, x, r -> a != null && a.size == 2 && !(x + 5 > 20) && r.getOrNull() == 0.toDouble() },
            { a, x, r -> a != null && a.size == 2 && x + 5 > 20 && r.getOrNull() == 1.toDouble() }
        )
    }

    @Test
    fun testBooleanArray() {
        checkWithException(
            PrimitiveArrays::booleanArray,
            eq(4),
            { a, _, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, _, r -> a != null && a.size != 2 && r.getOrNull() == -1 },
            { a, x, y, r -> a != null && a.size == 2 && !(x xor y) && r.getOrNull() == 0 },
            { a, x, y, r -> a != null && a.size == 2 && (x xor y) && r.getOrNull() == 1 }
        )
    }

    @Test
    fun testByteSizeAndIndex() {
        check(
            PrimitiveArrays::byteSizeAndIndex,
            eq(5),
            { a, _, r -> a == null && r == (-1).toByte() },
            { a, x, r -> a != null && a.size <= x.toInt() && r == (-1).toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() < 1 && r == (-1).toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() > 0 && x + 5 <= 7 && r == 0.toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() > 0 && x + 5 > 7 && r == 1.toByte() }
        )
    }

    @Test
    fun testShortSizeAndIndex() {
        check(
            PrimitiveArrays::shortSizeAndIndex,
            eq(5),
            { a, _, r -> a == null && r == (-1).toByte() },
            { a, x, r -> a != null && a.size <= x.toInt() && r == (-1).toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() < 1 && r == (-1).toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() > 0 && x + 5 <= 7 && r == 0.toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() > 0 && x + 5 > 7 && r == 1.toByte() }
        )
    }

    @Test
    fun testCharSizeAndIndex() {
        check(
            PrimitiveArrays::charSizeAndIndex,
            eq(5),
            { a, _, r -> a == null && r == (-1).toByte() },
            { a, x, r -> a != null && a.size <= x.toInt() && r == (-1).toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() < 1 && r == (-1).toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() > 0 && x + 5 <= 7.toChar() && r == 0.toByte() },
            { a, x, r -> a != null && a.size > x.toInt() && x.toInt() > 0 && x + 5 > 7.toChar() && r == 1.toByte() }
        )
    }
}