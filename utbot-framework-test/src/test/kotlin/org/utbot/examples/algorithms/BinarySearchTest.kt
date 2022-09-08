package org.utbot.examples.algorithms

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.junit.jupiter.api.Test

class BinarySearchTest : UtValueTestCaseChecker(testClass = BinarySearch::class,) {
    @Test
    fun testLeftBinarySearch() {
        val fullSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("does not iterate "),
                    DocCodeStmt("while(left < right - 1)"),
                    DocRegularStmt(", "),
                    DocRegularStmt("executes conditions:\n"),
                    DocRegularStmt("    "),
                    DocCodeStmt("(found): False"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return -1;"),
                    DocRegularStmt("\n")

                )
            )
        )
        checkWithException(
            BinarySearch::leftBinSearch,
            ignoreExecutionsNumber,
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a.size >= 2 && a[0] > a[1] && r.isException<IllegalArgumentException>() },
            { a, _, r -> a.isEmpty() && r.getOrNull() == -1 },
            { a, key, r -> a.isNotEmpty() && key >= a[(a.size - 1) / 2] && key !in a && r.getOrNull() == -1 },
            { a, key, r -> a.isNotEmpty() && key in a && r.getOrNull() == a.indexOfFirst { it == key } + 1 },
            // TODO enable it JIRA:1442
            /*
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(found): False")),
                keyContain(DocCodeStmt("(found): True")),
                keyContain(DocRegularStmt("    BinarySearch::isUnsorted once")),
                keyContain(DocRegularStmt("throws NullPointerException in: isUnsorted(array)")),
                keyContain(DocRegularStmt("throws IllegalArgumentException after condition: isUnsorted(array)")),
                keyContain(DocCodeStmt("(array[middle] < key): True")),
                keyContain(DocCodeStmt("(array[middle] == key): True")),
                keyContain(DocCodeStmt("(array[middle] < key): True")),
                keyMatch(fullSummary)
            ),
            summaryNameChecks = listOf(
                keyContain("testLeftBinSearch_BinarySearchIsUnsorted"),
                keyContain("testLeftBinSearch_ThrowIllegalArgumentException"),
                keyContain("testLeftBinSearch_NotFound"),
                keyContain("testLeftBinSearch_MiddleOfArrayLessThanKey"),
                keyContain("testLeftBinSearch_Found")
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("isUnsorted(array) -> ThrowIllegalArgumentException"),
                keyMatch("isUnsorted(array) -> ThrowIllegalArgumentException"),
                keyMatch("found : False -> return -1"),
                keyMatch("array[middle] == key : True -> return right + 1"),
                keyMatch("array[middle] < key : True -> return -1")
            )
             */
        )
    }

    @Test
    fun testRightBinarySearch() {
        checkWithException(
            BinarySearch::rightBinSearch,
            ignoreExecutionsNumber,
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a.isEmpty() && r.getOrNull() == -1 },
            { a, _, r -> a.size >= 2 && a[0] > a[1] && r.isException<IllegalArgumentException>() },
            { a, key, r -> a.isNotEmpty() && key !in a && r.getOrNull() == -1 },
            { a, key, r -> a.isNotEmpty() && key in a && r.getOrNull() == a.indexOfLast { it == key } + 1 }
        )
    }

    @Test
    fun testDefaultBinarySearch() {
        checkWithException(
            BinarySearch::defaultBinarySearch,
            ignoreExecutionsNumber,
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a.isEmpty() && r.getOrNull() == -1 },
            { a, _, r -> a.size >= 2 && a[0] > a[1] && r.isException<IllegalArgumentException>() },
            { a, key, r -> a.isNotEmpty() && key < a.first() && r.getOrNull() == a.binarySearch(key) },
            { a, key, r -> a.isNotEmpty() && key == a.first() && r.getOrNull() == a.binarySearch(key) },
        )
    }
}