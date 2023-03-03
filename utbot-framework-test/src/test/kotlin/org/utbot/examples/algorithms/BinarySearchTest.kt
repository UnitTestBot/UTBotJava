package org.utbot.examples.algorithms

import org.junit.jupiter.api.Test
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

class BinarySearchTest : UtValueTestCaseChecker(testClass = BinarySearch::class,) {
    @Test
    fun testLeftBinarySearch() {
        checkWithException(
            BinarySearch::leftBinSearch,
            ignoreExecutionsNumber,
            { a, _, r -> a == null && r.isException<NullPointerException>() },
            { a, _, r -> a.size >= 2 && a[0] > a[1] && r.isException<IllegalArgumentException>() },
            { a, _, r -> a.isEmpty() && r.getOrNull() == -1 },
            { a, key, r -> a.isNotEmpty() && key >= a[(a.size - 1) / 2] && key !in a && r.getOrNull() == -1 },
            { a, key, r -> a.isNotEmpty() && key in a && r.getOrNull() == a.indexOfFirst { it == key } + 1 }
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