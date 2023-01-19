package org.utbot.examples.arrays

import org.junit.jupiter.api.Test
import org.utbot.testing.AtLeast
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

class CopyOfExampleTest : UtValueTestCaseChecker(testClass = CopyOfExample::class) {
    @Test
    fun testCopyOf() {
        checkWithException(
            CopyOfExample::copyOfExample,
            ignoreExecutionsNumber,
            { _, l, r -> l < 0 && r.isException<NegativeArraySizeException>() },
            { arr, l, r -> arr.copyOf(l).contentEquals(r.getOrThrow()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testCopyOfRange() {
        checkWithException(
            CopyOfExample::copyOfRangeExample,
            ignoreExecutionsNumber,
            { _, from, _, r -> from < 0 && r.isException<ArrayIndexOutOfBoundsException>() },
            { arr, from, _, r -> from > arr.size && r.isException<ArrayIndexOutOfBoundsException>() },
            { _, from, to, r -> from > to && r.isException<IllegalArgumentException>() },
            { arr, from, to, r -> arr.copyOfRange(from, to).contentEquals(r.getOrThrow()) },
            coverage = AtLeast(82)
        )
    }
}