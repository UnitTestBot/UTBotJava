package org.utbot.examples.algorithms

import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.jupiter.api.Test
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testing.*

// TODO Kotlin mocks generics https://github.com/UnitTestBot/UTBotJava/issues/88
internal class SortTest : UtValueTestCaseChecker(
    testClass = Sort::class,
    testCodeGeneration = true,
    configurations = listOf(
        Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.DO_NOT_PARAMETRIZE, TestExecution),
        Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.PARAMETRIZE, TestExecution),
        Configuration(CodegenLanguage.KOTLIN, ParametrizedTestSource.DO_NOT_PARAMETRIZE, CodeGeneration),
    )
) {
    @Test
    fun testQuickSort() {
        check(
            Sort::quickSort,
            ignoreExecutionsNumber,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }

    @Test
    fun testSwap() {
        checkWithException(
            Sort::swap,
            ge(4),
            { a, _, _, r -> a == null && r.isException<NullPointerException>() },
            { a, i, _, r -> a != null && (i < 0 || i >= a.size) && r.isException<IndexOutOfBoundsException>() },
            { a, i, j, r -> a != null && i in a.indices && (j < 0 || j >= a.size) && r.isException<IndexOutOfBoundsException>() },
            { a, i, j, _ -> a != null && i in a.indices && j in a.indices }
        )
    }

    @Test
    fun testArrayCopy() {
        check(
            Sort::arrayCopy,
            eq(1),
            { r -> r contentEquals intArrayOf(1, 2, 3) }
        )
    }

    @Test
    fun testMergeSort() {
        check(
            Sort::mergeSort,
            eq(4),
            { a, r -> a == null && r == null },
            { a, r -> a != null && r != null && a.size < 2 },
            { a, r ->
                require(a is IntArray && r is IntArray)

                val sortedConstraint = a.size >= 2 && a.sorted() == r.toList()

                val maxInLeftHalf = a.slice(0 until a.size / 2).maxOrNull()!!
                val maxInRightHalf = a.slice(a.size / 2 until a.size).maxOrNull()!!

                sortedConstraint && maxInLeftHalf >= maxInRightHalf
            },
            { a, r ->
                require(a is IntArray && r is IntArray)

                val sortedConstraint = a.size >= 2 && a.sorted() == r.toList()

                val maxInLeftHalf = a.slice(0 until a.size / 2).maxOrNull()!!
                val maxInRightHalf = a.slice(a.size / 2 until a.size).maxOrNull()!!

                sortedConstraint && maxInLeftHalf < maxInRightHalf
            },
        )
    }

    @Test
    fun testMerge() {
        checkWithException(
            Sort::merge,
            eq(6),
            { lhs, _, r -> lhs == null && r.isException<NullPointerException>() },
            { lhs, rhs, r -> lhs != null && lhs.isEmpty() && r.getOrNull() contentEquals rhs },
            { lhs, rhs, _ -> lhs != null && lhs.isNotEmpty() && rhs == null },
            { lhs, rhs, r ->
                val lhsCondition = lhs != null && lhs.isNotEmpty()
                val rhsCondition = rhs != null && rhs.isEmpty()
                val connection = r.getOrNull() contentEquals lhs

                lhsCondition && rhsCondition && connection
            },
            { lhs, rhs, r ->
                val lhsCondition = lhs != null && lhs.isNotEmpty()
                val rhsCondition = rhs != null && rhs.isNotEmpty()
                val connection = lhs.last() < rhs.last() && r.getOrNull()?.toList() == (lhs + rhs).sorted()

                lhsCondition && rhsCondition && connection
            },
            { lhs, rhs, r ->
                val lhsCondition = lhs != null && lhs.isNotEmpty()
                val rhsCondition = rhs != null && rhs.isNotEmpty()
                val connection = lhs.last() >= rhs.last() && r.getOrNull()?.toList() == (lhs + rhs).sorted()

                lhsCondition && rhsCondition && connection
            }
        )
    }

    @Test
    fun testDefaultSort() {
        checkWithException(
            Sort::defaultSort,
            eq(3),
            { a, r -> a == null && r.isException<NullPointerException>() },
            { a, r -> a != null && a.size < 4 && r.isException<IllegalArgumentException>() },
            { a, r ->
                val resultArray = intArrayOf(-100, 0, 100, 200)
                a != null && r.getOrNull()!!.size >= 4 && r.getOrNull() contentEquals resultArray
            }
        )
    }
}