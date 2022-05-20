package org.utbot.examples.math

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.AtLeast
import org.utbot.examples.algorithms.Sort
import org.utbot.examples.eq
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.isException
import org.utbot.examples.withSolverTimeoutInMillis
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.Compilation
import org.utbot.framework.plugin.api.CodegenLanguage
import kotlin.math.floor
import kotlin.math.sqrt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class OverflowAsErrorTest : AbstractTestCaseGeneratorTest(
    testClass = OverflowExamples::class,
    testCodeGeneration = true,
    // Don't launch tests, because ArithmeticException will be expected, but it is not supposed to be actually thrown.
    // ArithmeticException acts as a sign of Overflow.
    listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA, Compilation),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, Compilation),
    )
) {
    private val treatOverflowAsError = UtSettings.treatOverflowAsError

    @BeforeAll
    fun beforeAll() {
        UtSettings.treatOverflowAsError = true
    }

    @AfterAll
    fun afterAll() {
        UtSettings.treatOverflowAsError = treatOverflowAsError
    }

    @Test
    fun testIntOverflow() {
        checkWithException(
            OverflowExamples::intOverflow,
            eq(5),
            { x, _, r -> x * x * x <= 0 && x > 0 && r.isException<ArithmeticException>() }, // through overflow
            { x, _, r -> x * x * x <= 0 && x > 0 && r.isException<ArithmeticException>() }, // through overflow (2nd '*')
            { x, _, r -> x * x * x >= 0 && x >= 0 && r.getOrNull() == 0 },
            { x, y, r -> x * x * x > 0 && x > 0 && y == 10 && r.getOrNull() == 1 },
            { x, y, r -> x * x * x > 0 && x > 0 && y != 10 && r.getOrNull() == 0 },
            coverage = AtLeast(90),
        )
    }

    @Test
    fun testByteAddOverflow() {
        checkWithException(
            OverflowExamples::byteAddOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>() },
            { x, y, r ->
                val negOverflow = ((x + y).toByte() >= 0 && x < 0 && y < 0)
                val posOverflow = ((x + y).toByte() <= 0 && x > 0 && y > 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testByteSubOverflow() {
        checkWithException(
            OverflowExamples::byteSubOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>()},
            { x, y, r ->
                val negOverflow = ((x - y).toByte() >= 0 && x < 0 && y > 0)
                val posOverflow = ((x - y).toByte() <= 0 && x > 0 && y < 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()}, // through overflow
        )
    }

    @Test
    fun testByteMulOverflow() {
        checkWithException(
            OverflowExamples::byteMulOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>()},
            { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
        )
    }

    @Test
    fun testShortAddOverflow() {
        checkWithException(
            OverflowExamples::shortAddOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>() },
            { x, y, r ->
                val negOverflow = ((x + y).toShort() >= 0 && x < 0 && y < 0)
                val posOverflow = ((x + y).toShort() <= 0 && x > 0 && y > 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testShortSubOverflow() {
        checkWithException(
            OverflowExamples::shortSubOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>()},
            { x, y, r ->
                val negOverflow = ((x - y).toShort() >= 0 && x < 0 && y > 0)
                val posOverflow = ((x - y).toShort() <= 0 && x > 0 && y < 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testShortMulOverflow() {
        checkWithException(
            OverflowExamples::shortMulOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>()},
            { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
        )
    }

    @Test
    fun testIntAddOverflow() {
        checkWithException(
            OverflowExamples::intAddOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>() },
            { x, y, r ->
                val negOverflow = ((x + y) >= 0 && x < 0 && y < 0)
                val posOverflow = ((x + y) <= 0 && x > 0 && y > 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testIntSubOverflow() {
        checkWithException(
            OverflowExamples::intSubOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>()},
            { x, y, r ->
                val negOverflow = ((x - y) >= 0 && x < 0 && y > 0)
                val posOverflow = ((x - y) <= 0 && x > 0 && y < 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testIntMulOverflow() {
        // This test has solver timeout.
        // Reason: softConstraints, containing limits for Int values, hang solver.
        // With solver timeout softConstraints are dropped and hard constraints are SAT for overflow.
        withSolverTimeoutInMillis(timeoutInMillis = 1000) {
            checkWithException(
                OverflowExamples::intMulOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

    @Test
    fun testLongAddOverflow() {
        checkWithException(
            OverflowExamples::longAddOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>() },
            { x, y, r ->
                val negOverflow = ((x + y) >= 0 && x < 0 && y < 0)
                val posOverflow = ((x + y) <= 0 && x > 0 && y > 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testLongSubOverflow() {
        checkWithException(
            OverflowExamples::longSubOverflow,
            eq(2),
            { _, _, r -> !r.isException<ArithmeticException>()},
            { x, y, r ->
                val negOverflow = ((x - y) >= 0 && x < 0 && y > 0)
                val posOverflow = ((x - y) <= 0 && x > 0 && y < 0)
                (negOverflow || posOverflow) && r.isException<ArithmeticException>()
            }, // through overflow
        )
    }

    @Test
    fun testLongMulOverflow() {
        // This test has solver timeout.
        // Reason: softConstraints, containing limits for Int values, hang solver.
        // With solver timeout softConstraints are dropped and hard constraints are SAT for overflow.
        withSolverTimeoutInMillis(timeoutInMillis = 2000) {
            checkWithException(
                OverflowExamples::longMulOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

    @Test
    fun testIncOverflow() {
        checkWithException(
            OverflowExamples::incOverflow,
            eq(2),
            { _, r -> !r.isException<ArithmeticException>()},
            { _, r -> r.isException<ArithmeticException>() }, // through overflow
        )
    }

    @Test
    fun testIntCubeOverflow() {
        val sqrtIntMax = floor(sqrt(Int.MAX_VALUE.toDouble())).toInt()
        checkWithException(
            OverflowExamples::intCubeOverflow,
            eq(3),
            { _, r -> !r.isException<ArithmeticException>()},
            // Can't use abs(x) below, because abs(Int.MIN_VALUE) == Int.MIN_VALUE.
            // (Int.MAX_VALUE shr 16) is the border of square overflow and cube overflow.
            // Int.MAX_VALUE.toDouble().pow(1/3.toDouble())
            { x, r -> (x > -sqrtIntMax && x < sqrtIntMax ) && r.isException<ArithmeticException>() }, // through overflow
            { x, r -> (x <= -sqrtIntMax || x >= sqrtIntMax) && r.isException<ArithmeticException>() }, // through overflow
        )
    }

//  Generated Kotlin code does not compile, so disabled for now
    @Test
    @Disabled
    fun testQuickSort() {
        checkWithException(
            Sort::quickSort,
            ignoreExecutionsNumber,
            { _, _, _, r -> !r.isException<ArithmeticException>()},
            { _, _, _, r -> r.isException<ArithmeticException>() }, // through overflow
        )
    }
}
