package org.utbot.examples.math

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.examples.algorithms.Sort
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withSolverTimeoutInMillis
import org.utbot.testcheckers.withTreatingOverflowAsError
import org.utbot.tests.infrastructure.Compilation
import kotlin.math.floor
import kotlin.math.sqrt

internal class OverflowAsErrorTest : UtValueTestCaseChecker(
    testClass = OverflowExamples::class,
    testCodeGeneration = true,
    // Don't launch tests, because ArithmeticException will be expected, but it is not supposed to be actually thrown.
    // ArithmeticException acts as a sign of Overflow.
    listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA, Compilation),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, Compilation),
    )
) {
    @Test
    fun testIntOverflow() {
        withTreatingOverflowAsError {
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
    }

    @Test
    fun testByteAddOverflow() {
        withTreatingOverflowAsError {
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
    }

    @Test
    fun testByteSubOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::byteSubOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { x, y, r ->
                    val negOverflow = ((x - y).toByte() >= 0 && x < 0 && y > 0)
                    val posOverflow = ((x - y).toByte() <= 0 && x > 0 && y < 0)
                    (negOverflow || posOverflow) && r.isException<ArithmeticException>()
                }, // through overflow
            )
        }
    }

    @Test
    fun testByteMulOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::byteMulOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

    @Test
    fun testShortAddOverflow() {
        withTreatingOverflowAsError {
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
    }

    @Test
    fun testShortSubOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::shortSubOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { x, y, r ->
                    val negOverflow = ((x - y).toShort() >= 0 && x < 0 && y > 0)
                    val posOverflow = ((x - y).toShort() <= 0 && x > 0 && y < 0)
                    (negOverflow || posOverflow) && r.isException<ArithmeticException>()
                }, // through overflow
            )
        }
    }

    @Test
    fun testShortMulOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::shortMulOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

    @Test
    fun testIntAddOverflow() {
        withTreatingOverflowAsError {
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
    }

    @Test
    fun testIntSubOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::intSubOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { x, y, r ->
                    val negOverflow = ((x - y) >= 0 && x < 0 && y > 0)
                    val posOverflow = ((x - y) <= 0 && x > 0 && y < 0)
                    (negOverflow || posOverflow) && r.isException<ArithmeticException>()
                }, // through overflow
            )
        }
    }

    @Test
    fun testIntMulOverflow() {
        // This test has solver timeout.
        // Reason: softConstraints, containing limits for Int values, hang solver.
        // With solver timeout softConstraints are dropped and hard constraints are SAT for overflow.
        withSolverTimeoutInMillis(timeoutInMillis = 1000) {
            withTreatingOverflowAsError {
                checkWithException(
                    OverflowExamples::intMulOverflow,
                    eq(2),
                    { _, _, r -> !r.isException<ArithmeticException>() },
                    { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
                )
            }
        }
    }

    @Test
    fun testLongAddOverflow() {
        withTreatingOverflowAsError {
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
    }

    @Test
    fun testLongSubOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::longSubOverflow,
                eq(2),
                { _, _, r -> !r.isException<ArithmeticException>() },
                { x, y, r ->
                    val negOverflow = ((x - y) >= 0 && x < 0 && y > 0)
                    val posOverflow = ((x - y) <= 0 && x > 0 && y < 0)
                    (negOverflow || posOverflow) && r.isException<ArithmeticException>()
                }, // through overflow
            )
        }
    }

    @Test
    @Disabled("Flaky branch count mismatch (1 instead of 2)")
    fun testLongMulOverflow() {
        // This test has solver timeout.
        // Reason: softConstraints, containing limits for Int values, hang solver.
        // With solver timeout softConstraints are dropped and hard constraints are SAT for overflow.
        withSolverTimeoutInMillis(timeoutInMillis = 2000) {
            withTreatingOverflowAsError {
                checkWithException(
                    OverflowExamples::longMulOverflow,
                    eq(2),
                    { _, _, r -> !r.isException<ArithmeticException>() },
                    { _, _, r -> r.isException<ArithmeticException>() }, // through overflow
                )
            }
        }
    }

    @Test
    fun testIncOverflow() {
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::incOverflow,
                eq(2),
                { _, r -> !r.isException<ArithmeticException>() },
                { _, r -> r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

    @Test
    fun testIntCubeOverflow() {
        val sqrtIntMax = floor(sqrt(Int.MAX_VALUE.toDouble())).toInt()
        withTreatingOverflowAsError {
            checkWithException(
                OverflowExamples::intCubeOverflow,
                eq(3),
                { _, r -> !r.isException<ArithmeticException>() },
                // Can't use abs(x) below, because abs(Int.MIN_VALUE) == Int.MIN_VALUE.
                // (Int.MAX_VALUE shr 16) is the border of square overflow and cube overflow.
                // Int.MAX_VALUE.toDouble().pow(1/3.toDouble())
                { x, r -> (x > -sqrtIntMax && x < sqrtIntMax) && r.isException<ArithmeticException>() }, // through overflow
                { x, r -> (x <= -sqrtIntMax || x >= sqrtIntMax) && r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

//  Generated Kotlin code does not compile, so disabled for now
    @Test
    @Disabled
    fun testQuickSort() {
        withTreatingOverflowAsError {
            checkWithException(
                Sort::quickSort,
                ignoreExecutionsNumber,
                { _, _, _, r -> !r.isException<ArithmeticException>() },
                { _, _, _, r -> r.isException<ArithmeticException>() }, // through overflow
            )
        }
    }

    @Test
    fun testIntOverflowWithoutError() {
        check(
            OverflowExamples::intOverflow,
            eq(6),
            { x, _, r -> x * x * x <= 0 && x <= 0 && r == 0 },
            { x, _, r -> x * x * x > 0 && x <= 0 && r == 0 }, // through overflow
            { x, y, r -> x * x * x > 0 && x > 0 && y != 10 && r == 0 },
            { x, y, r -> x * x * x > 0 && x > 0 && y == 10 && r == 1 },
            { x, y, r -> x * x * x <= 0 && x > 0 && y != 20 && r == 0 }, // through overflow
            { x, y, r -> x * x * x <= 0 && x > 0 && y == 20 && r == 2 } // through overflow
        )
    }
}
