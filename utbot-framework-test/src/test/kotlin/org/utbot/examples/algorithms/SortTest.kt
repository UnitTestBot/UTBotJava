package org.utbot.examples.algorithms

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.MockStrategyApi
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge

internal class SortTest : UtValueTestCaseChecker(testClass = Sort::class) {
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
            },
        )
    }

    @Test
    fun testDefaultSort() {
        val defaultSortSummary1 = listOf(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("\n"),
                    DocRegularStmt("throws NullPointerException in: array.length < 4"),
                    DocRegularStmt("\n")
                )
            )
        )

        val defaultSortSummary2 = listOf(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("executes conditions:\n"),
                    DocRegularStmt("    "),
                    DocCodeStmt("(array.length < 4): True"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("throws IllegalArgumentException after condition: array.length < 4"),
                    DocRegularStmt("\n")
                )
            )
        )
        val defaultSortSummary3 = listOf(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("executes conditions:\n"),
                    DocRegularStmt("    "),
                    DocCodeStmt("(array.length < 4): False"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("invokes:\n"),
                    DocRegularStmt("    {@link java.util.Arrays#sort(int[])} once"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return array;"),
                    DocRegularStmt("\n")
                )
            )
        )
        checkWithException(
            Sort::defaultSort,
            eq(3),
            { a, r -> a == null && r.isException<NullPointerException>() },
            { a, r -> a != null && a.size < 4 && r.isException<IllegalArgumentException>() },
            { a, r ->
                val resultArray = intArrayOf(-100, 0, 100, 200)
                a != null && r.getOrNull()!!.size >= 4 && r.getOrNull() contentEquals resultArray
            },
            summaryTextChecks = listOf(
                keyMatch(defaultSortSummary1),
                keyMatch(defaultSortSummary2),
                keyMatch(defaultSortSummary3),
            ),
            summaryNameChecks = listOf(
                keyMatch("testDefaultSort_ThrowNullPointerException"),
                keyMatch("testDefaultSort_ArrayLengthLessThan4"),
                keyMatch("testDefaultSort_ArrayLengthGreaterOrEqual4"),
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("array.length < 4 -> ThrowNullPointerException"),
                keyMatch("array.length < 4 -> ThrowIllegalArgumentException"),
                keyMatch("array.length < 4 : False -> return array"),
            )
        )
    }
}