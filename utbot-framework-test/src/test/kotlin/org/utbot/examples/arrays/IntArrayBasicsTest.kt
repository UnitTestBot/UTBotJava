package org.utbot.examples.arrays

import org.junit.jupiter.api.Disabled
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class IntArrayBasicsTest : UtValueTestCaseChecker(
    testClass = IntArrayBasics::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testIntArrayWithAssumeOrExecuteConcretely() {
        check(
            IntArrayBasics::intArrayWithAssumeOrExecuteConcretely,
            eq(4),
            { x, n, r -> x > 0 && n < 20 && r?.size == 2 },
            { x, n, r -> x > 0 && n >= 20 && r?.size == 4 },
            { x, n, r -> x <= 0 && n < 20 && r?.size == 10 },
            { x, n, r -> x <= 0 && n >= 20 && r?.size == 20 },
        )
    }

    @Test
    fun testInitArray() {
        checkWithException(
            IntArrayBasics::initAnArray,
            eq(4),
            { n, r -> n < 0 && r.isException<NegativeArraySizeException>() },
            { n, r -> n == 0 && r.isException<IndexOutOfBoundsException>() },
            { n, r -> n == 1 && r.isException<IndexOutOfBoundsException>() },
            { n, r ->
                val resultArray = IntArray(n) { if (it == n - 1 || it == n - 2) it else 0 }
                n > 1 && r.getOrNull() contentEquals resultArray
            }
        )
    }

    @Test
    fun testIsValid() {
        checkWithException(
            IntArrayBasics::isValid,
            ignoreExecutionsNumber,
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, n, r -> a != null && (n < 0 || n >= a.size) && r.isException<IndexOutOfBoundsException>() },
            { a, n, r -> a != null && n in a.indices && a[n] == 9 && n == 5 && r.getOrNull() == true },
            { a, n, r -> a != null && n in a.indices && !(a[n] == 9 && n == 5) && r.getOrNull() == false },
            { a, n, r -> a != null && n in a.indices && a[n] > 9 && n == 5 && r.getOrNull() == true },
            { a, n, r -> a != null && n in a.indices && !(a[n] > 9 && n == 5) && r.getOrNull() == false },
        )
    }

    @Test
    fun testGetValue() {
        checkWithException(
            IntArrayBasics::getValue,
            ge(4),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, n, r -> a != null && a.size > 6 && (n < 0 || n >= a.size) && r.isException<IndexOutOfBoundsException>() },
            { a, n, r -> a != null && a.size > 6 && n < a.size && r.getOrNull() == a[n] },
            { a, _, r -> a != null && a.size <= 6 && r.getOrNull() == -1 }
        )
    }

    @Test
    fun testSetValue() {
        checkWithException(
            IntArrayBasics::setValue,
            eq(5),
            { _, x, r -> x <= 0 && r.getOrNull() == 0 },
            { a, x, r -> x > 0 && a == null && r.isException<NullPointerException>() },
            { a, x, r -> x > 0 && a != null && a.isEmpty() && r.getOrNull() == 0 },
            { a, x, r -> x in 1..2 && a != null && a.isNotEmpty() && r.getOrNull() == 1 },
            { a, x, r -> x > 2 && a != null && a.isNotEmpty() && r.getOrNull() == 2 }
        )
    }

    @Test
    fun testCheckFour() {
        checkWithException(
            IntArrayBasics::checkFour,
            eq(7),
            { a, r -> a == null && r.isException<NullPointerException>() },
            { a, r -> a != null && a.size < 4 && r.getOrNull() == -1 },
            { a, r -> a != null && a.size >= 4 && a[0] != 1 && r.getOrNull() == 0 },
            { a, r -> a != null && a.size >= 4 && a[0] == 1 && a[1] != 2 && r.getOrNull() == 0 },
            { a, r -> a != null && a.size >= 4 && a[0] == 1 && a[1] == 2 && a[2] != 3 && r.getOrNull() == 0 },
            { a, r -> a != null && a.size >= 4 && a[0] == 1 && a[1] == 2 && a[2] == 3 && a[3] != 4 && r.getOrNull() == 0 },
            { a, r ->
                require(a != null)

                val precondition = a.size >= 4 && a[0] == 1 && a[1] == 2 && a[2] == 3 && a[3] == 4
                val postcondition = r.getOrNull() == IntArrayBasics().checkFour(a)

                precondition && postcondition
            }
        )
    }

    @Test
    fun testNullability() {
        check(
            IntArrayBasics::nullability,
            eq(3),
            { a, r -> a == null && r == 1 },
            { a, r -> a != null && a.size > 1 && r == 2 },
            { a, r -> a != null && a.size in 0..1 && r == 3 },
        )
    }

    @Test
    fun testEquality() {
        check(
            IntArrayBasics::equality,
            eq(4),
            { a, _, r -> a == null && r == 1 },
            { a, b, r -> a != null && b == null && r == 1 },
            { a, b, r -> a != null && b != null && a.size == b.size && r == 2 },
            { a, b, r -> a != null && b != null && a.size != b.size && r == 3 },
        )
    }

    @Test
    fun testOverwrite() {
        checkWithException(
            IntArrayBasics::overwrite,
            eq(5),
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a != null && a.size != 1 && r.getOrNull() == 0 },
            { a, b, r -> a != null && a.size == 1 && a[0] > 0 && b < 0 && r.getOrNull() == 1 },
            { a, b, r -> a != null && a.size == 1 && a[0] > 0 && b >= 0 && r.getOrNull() == 2 },
            { a, _, r -> a != null && a.size == 1 && a[0] <= 0 && r.getOrNull() == 3 },
        )
    }

    @Test
    fun testMergeArrays() {
        check(
            IntArrayBasics::mergeArrays,
            ignoreExecutionsNumber,
            { fst, _, _ -> fst == null },
            { fst, snd, _ -> fst != null && snd == null },
            { fst, snd, r -> fst != null && snd != null && fst.size < 2 && r == null },
            { fst, snd, r -> fst != null && snd != null && fst.size >= 2 && snd.size < 2 && r == null },
            { fst, snd, r ->
                require(fst != null && snd != null && r != null)

                val sizeConstraint = fst.size >= 2 && snd.size >= 2 && r.size == fst.size + snd.size
                val maxConstraint = fst.maxOrNull()!! < snd.maxOrNull()!!
                val contentConstraint = r contentEquals (IntArrayBasics().mergeArrays(fst, snd))

                sizeConstraint && maxConstraint && contentConstraint
            },
            { fst, snd, r ->
                require(fst != null && snd != null && r != null)

                val sizeConstraint = fst.size >= 2 && snd.size >= 2 && r.size == fst.size + snd.size
                val maxConstraint = fst.maxOrNull()!! >= snd.maxOrNull()!!
                val contentConstraint = r contentEquals (IntArrayBasics().mergeArrays(fst, snd))

                sizeConstraint && maxConstraint && contentConstraint
            }
        )
    }

    @Test
    fun testNewArrayInTheMiddle() {
        check(
            IntArrayBasics::newArrayInTheMiddle,
            eq(5),
            { a, _ -> a == null },
            { a, _ -> a != null && a.isEmpty() },
            { a, _ -> a != null && a.size < 2 },
            { a, _ -> a != null && a.size < 3 },
            { a, r -> a != null && a.size >= 3 && r != null && r[0] == 1 && r[1] == 2 && r[2] == 3 }
        )
    }

    @Test
    fun testNewArrayInTheMiddleMutation() {
        checkParamsMutations(
            IntArrayBasics::newArrayInTheMiddle,
            ignoreExecutionsNumber,
            { _, arrayAfter -> arrayAfter[0] == 1 && arrayAfter[1] == 2 && arrayAfter[2] == 3 }
        )
    }

    @Test
    fun testReversed() {
        check(
            IntArrayBasics::reversed,
            eq(5),
            { a, _ -> a == null },
            { a, _ -> a != null && a.size != 3 },
            { a, r -> a != null && a.size == 3 && a[0] <= a[1] && r == null },
            { a, r -> a != null && a.size == 3 && a[0] > a[1] && a[1] <= a[2] && r == null },
            { a, r -> a != null && a.size == 3 && a[0] > a[1] && a[1] > a[2] && r contentEquals a.reversedArray() },
        )
    }

    @Test
    fun testUpdateCloned() {
        check(
            IntArrayBasics::updateCloned,
            eq(3),
            { a, _ -> a == null },
            { a, _ -> a.size != 3 },
            { a, r -> a.size == 3 && r != null && r.toList() == a.map { it * 2 } },
        )
    }

    @Test
    @Disabled("Java 11 transition -- Sergei is looking into it")
    fun testArraysEqualsExample() {
        check(
            IntArrayBasics::arrayEqualsExample,
            eq(2),
            { a, r -> a.size == 3 && a contentEquals intArrayOf(1, 2, 3) && r == 1 },
            { a, r -> !(a contentEquals intArrayOf(1, 2, 3)) && r == 2 }
        )
    }
}